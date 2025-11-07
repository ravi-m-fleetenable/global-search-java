-- ============================================================================
-- Global Search - Database Schema Migration V1
-- Enable PostgreSQL Extensions
-- ============================================================================

-- Extension: pg_trgm (Trigram similarity for fuzzy search)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Extension: unaccent (Remove accents for multilingual search)
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Extension: btree_gin (Multi-column GIN indexes)
CREATE EXTENSION IF NOT EXISTS btree_gin;

-- Extension: uuid-ossp (UUID generation)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Configure pg_trgm default thresholds
-- These can be overridden per session/query
SET pg_trgm.similarity_threshold = 0.3;      -- Global similarity
SET pg_trgm.word_similarity_threshold = 0.6; -- Word similarity

-- Verify extensions are installed
SELECT extname, extversion
FROM pg_extension
WHERE extname IN ('pg_trgm', 'unaccent', 'btree_gin', 'uuid-ossp');
