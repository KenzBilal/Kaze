-- arcs table
CREATE TABLE arcs (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    aliases TEXT NOT NULL DEFAULT '',
    cover_url TEXT,
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- arc_items table
CREATE TABLE arc_items (
    id TEXT PRIMARY KEY,
    arc_id TEXT NOT NULL REFERENCES arcs(id) ON DELETE CASCADE,
    order_index DOUBLE PRECISION NOT NULL,
    imdb_id TEXT NOT NULL,
    title TEXT NOT NULL,
    year INTEGER NOT NULL,
    type TEXT NOT NULL, -- 'MOVIE' or 'SERIES'
    poster_url TEXT,
    total_seasons INTEGER,
    start_season INTEGER,
    start_episode INTEGER,
    end_season INTEGER,
    end_episode INTEGER,
    phase_label TEXT,
    notes TEXT,
    is_optional BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_arcs_published ON arcs(is_published);
CREATE INDEX idx_arc_items_arc_id ON arc_items(arc_id);
CREATE INDEX idx_arc_items_order ON arc_items(arc_id, order_index);
CREATE INDEX idx_arc_items_imdb_id ON arc_items(arc_id, imdb_id);
