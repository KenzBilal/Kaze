import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }
  
  try {
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      { global: { headers: { Authorization: req.headers.get('Authorization')! } } }
    )

    const { prompt, username } = await req.json()
    
    // Only allow admin (kenzbilal)
    if (username !== 'kenzbilal') {
      return new Response(JSON.stringify({ error: 'Forbidden' }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 403 })
    }

    if (!prompt) {
      return new Response(JSON.stringify({ error: 'Missing prompt' }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 })
    }

    // GEMINI CALL
    const GEMINI_API_KEY = Deno.env.get('GEMINI_API_KEY') ?? ""

    const systemInstruction = `You are a movie and TV franchise expert. The user wants to build a watch order arc. 
You must return exactly a JSON object matching this schema, no markdown blocks, no conversational text.
{
  "arc_name": "Professional name of the franchise/arc (e.g., 'Marvel Cinematic Universe', 'Dexter Universe')",
  "arc_description": "Short 1-2 line summary.",
  "arc_aliases": "Extensive, highly powerful comma-separated aliases for search. Include common misspellings, abbreviations, character names, and sub-franchises.",
  "items": [
    {
      "type": "movie",
      "title": "Exact Movie Title",
      "year": 2008
    },
    {
      "type": "series",
      "title": "Exact Series Title",
      "year": 2010,
      "start_season": 1,
      "end_season": 3
    }
  ]
}
For series, if the whole series is meant, provide start_season 1 and end_season as the final season.
If it's only a specific episode crossover, you can optionally include start_episode and end_episode.
CRITICAL RULES:
1. Focus on MODERN, directly connected movies and series. EXCLUDE very old, disconnected adaptations or unrelated comic spin-offs unless they are part of the modern connected universe.
2. INCLUDE major modern animated series (e.g., 'What If...?', 'Spider-Verse').
3. INCLUDE officially announced upcoming titles (e.g., 'Dexter: Resurrection').
Output ONLY valid JSON.`

    const geminiRes = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ role: "user", parts: [{ text: prompt }] }],
        systemInstruction: { parts: [{ text: systemInstruction }] },
        generationConfig: {
          responseMimeType: "application/json",
          temperature: 0.1
        }
      })
    })

    if (!geminiRes.ok) {
      const err = await geminiRes.text()
      console.error("Gemini Error:", err)
      throw new Error("Failed to generate from Gemini")
    }

    const geminiData = await geminiRes.json()
    const textOutput = geminiData.candidates?.[0]?.content?.parts?.[0]?.text
    if (!textOutput) throw new Error("No output from Gemini")

    let parsed
    try {
      parsed = JSON.parse(textOutput)
    } catch (e) {
      // Strip markdown backticks if Gemini ignored instructions
      const stripped = textOutput.replace(/```json/g, '').replace(/```/g, '').trim()
      parsed = JSON.parse(stripped)
    }

    const omdbApiKey = "73bc2eaa"

    // Fetch OMDB data for each item in parallel
    const arcItems = []
    let coverUrl = ""

    const omdbPromises = parsed.items.map(async (item, i) => {
      if (!item.title) return null

      try {
        const typeParam = item.type === 'series' ? '&type=series' : '&type=movie'
        const yearParam = item.year ? `&y=${item.year}` : ''
        const url = `https://www.omdbapi.com/?apikey=${omdbApiKey}&t=${encodeURIComponent(item.title)}${yearParam}${typeParam}`
        const omdbRes = await fetch(url)
        
        if (!omdbRes.ok) return null
        const omdbData = await omdbRes.json()
        
        if (omdbData.Response === "False") {
          console.warn(`Title ${item.title} not found on OMDB. Skipping.`)
          return null
        }

        let totalSeasons = parseInt(omdbData.totalSeasons) || 1
        let startS = item.start_season
        let endS = item.end_season

        if (item.type === 'series') {
          if (!startS) startS = 1
          if (!endS) endS = totalSeasons
          if (endS > totalSeasons) endS = totalSeasons // Auto correct
        }

        return {
          id: crypto.randomUUID(),
          imdb_id: omdbData.imdbID,
          title: omdbData.Title,
          type: item.type === 'series' ? 'SERIES' : 'MOVIE',
          poster_url: omdbData.Poster !== "N/A" ? omdbData.Poster : null,
          year: parseInt(omdbData.Year) || null,
          start_season: item.type === 'series' ? startS : null,
          end_season: item.type === 'series' ? endS : null,
          start_episode: item.start_episode || null,
          end_episode: item.end_episode || null,
          total_seasons: item.type === 'series' ? totalSeasons : null,
          order_index: i,
          is_optional: false,
          notes: item.notes || null
        }
      } catch (err) {
        console.error(`OMDB fetch failed for ${item.title}:`, err)
        return null
      }
    })

    const results = await Promise.all(omdbPromises)
    
    for (const res of results) {
      if (res) {
        arcItems.push(res)
        if (!coverUrl && res.poster_url) {
          coverUrl = res.poster_url
        }
      }
    }

    if (arcItems.length === 0) {
      throw new Error("No valid items found from OMDB")
    }

    const arcId = crypto.randomUUID()
    const newArc = {
      id: arcId,
      name: parsed.arc_name || prompt,
      description: parsed.arc_description || "",
      aliases: parsed.arc_aliases || "",
      cover_url: coverUrl,
      is_published: false,
      owner_id: null // Admin official arc
    }

    // Insert Arc
    const { error: arcErr } = await supabaseClient.from('arcs').insert(newArc)
    if (arcErr) throw new Error("Failed to insert arc: " + arcErr.message)

    // Assign arc_id to items
    const finalItems = arcItems.map(it => ({ ...it, arc_id: arcId }))

    // Insert Arc Items
    const { error: itemsErr } = await supabaseClient.from('arc_items').insert(finalItems)
    if (itemsErr) {
      // rollback arc
      await supabaseClient.from('arcs').delete().eq('id', arcId)
      throw new Error("Failed to insert arc_items: " + itemsErr.message)
    }

    return new Response(JSON.stringify({ arcId }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })

  } catch (error) {
    console.error("Generate Arc Error:", error)
    return new Response(JSON.stringify({ error: error.message }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 })
  }
})
