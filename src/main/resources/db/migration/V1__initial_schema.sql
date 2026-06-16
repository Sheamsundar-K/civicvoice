-- ============================================================
-- CivicVoice Database Migration V1 – Initial Schema
-- PostGIS extension required for geo queries
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ────────────────────────────────────────────────────────────
-- 1. USERS
-- ────────────────────────────────────────────────────────────
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(20)  NOT NULL DEFAULT 'CITIZEN'
                    CHECK (role IN ('CITIZEN','AUTHORITY','NGO','ADMIN')),
    department    VARCHAR(100),           -- for AUTHORITY accounts
    ward          VARCHAR(100),           -- ward/zone the authority manages
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified   BOOLEAN NOT NULL DEFAULT FALSE,
    avatar_url    VARCHAR(500),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email  ON users(email);
CREATE INDEX idx_users_role   ON users(role);
CREATE INDEX idx_users_ward   ON users(ward);

-- ────────────────────────────────────────────────────────────
-- 2. ISSUES  (geo-tagged civic reports)
-- ────────────────────────────────────────────────────────────
CREATE TABLE issues (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title             VARCHAR(255) NOT NULL,
    description       TEXT         NOT NULL,
    category          VARCHAR(50)  NOT NULL
                        CHECK (category IN (
                            'ROAD','WATER','ELECTRICITY','SANITATION',
                            'STREET_LIGHT','SEWAGE','PARK','NOISE',
                            'ILLEGAL_CONSTRUCTION','OTHER'
                        )),
    priority          VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM'
                        CHECK (priority IN ('CRITICAL','HIGH','MEDIUM','LOW')),
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN (
                            'OPEN','ASSIGNED','IN_PROGRESS',
                            'RESOLVED','CLOSED','REJECTED'
                        )),
    latitude          DOUBLE PRECISION NOT NULL,
    longitude         DOUBLE PRECISION NOT NULL,
    address           VARCHAR(500),
    ward              VARCHAR(100),
    city              VARCHAR(100) NOT NULL,
    state             VARCHAR(100) NOT NULL,
    pin_code          VARCHAR(10),
    reporter_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_to_id    UUID         REFERENCES users(id) ON DELETE SET NULL,
    department        VARCHAR(100),
    upvote_count      INTEGER      NOT NULL DEFAULT 0,
    comment_count     INTEGER      NOT NULL DEFAULT 0,
    is_anonymous      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_duplicate_of   UUID         REFERENCES issues(id) ON DELETE SET NULL,
    ai_spam_score     DECIMAL(5,4) DEFAULT 0.0,   -- reserved for ML integration
    resolution_note   TEXT,
    sla_breach        BOOLEAN      NOT NULL DEFAULT FALSE,
    sla_deadline      TIMESTAMP WITH TIME ZONE,
    resolved_at       TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for geo-proximity queries
CREATE INDEX idx_issues_latitude   ON issues(latitude);
CREATE INDEX idx_issues_longitude  ON issues(longitude);
CREATE INDEX idx_issues_status     ON issues(status);
CREATE INDEX idx_issues_category   ON issues(category);
CREATE INDEX idx_issues_ward       ON issues(ward);
CREATE INDEX idx_issues_reporter   ON issues(reporter_id);
CREATE INDEX idx_issues_assigned   ON issues(assigned_to_id);
CREATE INDEX idx_issues_created    ON issues(created_at DESC);
CREATE INDEX idx_issues_city       ON issues(city);

-- ────────────────────────────────────────────────────────────
-- 3. ISSUE STATUS HISTORY  (full immutable audit trail)
-- ────────────────────────────────────────────────────────────
CREATE TABLE issue_status_history (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id    UUID        NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    old_status  VARCHAR(20),
    new_status  VARCHAR(20) NOT NULL,
    changed_by  UUID        NOT NULL REFERENCES users(id),
    note        TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ish_issue_id ON issue_status_history(issue_id);

-- ────────────────────────────────────────────────────────────
-- 4. ISSUE MEDIA
-- ────────────────────────────────────────────────────────────
CREATE TABLE issue_media (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id    UUID        NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    media_type  VARCHAR(10)  NOT NULL CHECK (media_type IN ('IMAGE','VIDEO')),
    file_size   BIGINT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_media_issue_id ON issue_media(issue_id);

-- ────────────────────────────────────────────────────────────
-- 5. ISSUE UPVOTES  (one per user per issue)
-- ────────────────────────────────────────────────────────────
CREATE TABLE issue_upvotes (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id   UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (issue_id, user_id)
);

CREATE INDEX idx_upvotes_issue ON issue_upvotes(issue_id);

-- ────────────────────────────────────────────────────────────
-- 6. ISSUE COMMENTS
-- ────────────────────────────────────────────────────────────
CREATE TABLE issue_comments (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id     UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    author_id    UUID NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    parent_id    UUID         REFERENCES issue_comments(id) ON DELETE CASCADE,
    content      TEXT NOT NULL,
    is_official  BOOLEAN NOT NULL DEFAULT FALSE,  -- TRUE for authority replies
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_issue_id  ON issue_comments(issue_id);
CREATE INDEX idx_comments_parent_id ON issue_comments(parent_id);

-- ────────────────────────────────────────────────────────────
-- 7. POLLS
-- ────────────────────────────────────────────────────────────
CREATE TABLE polls (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    created_by     UUID NOT NULL REFERENCES users(id),
    ward           VARCHAR(100),           -- NULL = city-wide
    city           VARCHAR(100) NOT NULL,
    status         VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('DRAFT','ACTIVE','CLOSED')),
    poll_type      VARCHAR(15) NOT NULL DEFAULT 'SINGLE_CHOICE'
                     CHECK (poll_type IN ('SINGLE_CHOICE','MULTI_CHOICE')),
    is_anonymous   BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ends_at        TIMESTAMP WITH TIME ZONE,
    total_votes    INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_polls_status   ON polls(status);
CREATE INDEX idx_polls_ward     ON polls(ward);
CREATE INDEX idx_polls_ends_at  ON polls(ends_at);

-- ────────────────────────────────────────────────────────────
-- 8. POLL OPTIONS
-- ────────────────────────────────────────────────────────────
CREATE TABLE poll_options (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    poll_id     UUID        NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    option_text VARCHAR(255) NOT NULL,
    vote_count  INTEGER     NOT NULL DEFAULT 0,
    display_order INTEGER   NOT NULL DEFAULT 0
);

CREATE INDEX idx_poll_options_poll_id ON poll_options(poll_id);

-- ────────────────────────────────────────────────────────────
-- 9. POLL VOTES  (one vote record per user per poll)
-- ────────────────────────────────────────────────────────────
CREATE TABLE poll_votes (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    poll_id    UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    option_id  UUID NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    user_id    UUID          REFERENCES users(id) ON DELETE CASCADE,  -- NULL if anonymous
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Prevent duplicate votes (per user per poll)
CREATE UNIQUE INDEX idx_poll_votes_unique ON poll_votes(poll_id, user_id)
    WHERE user_id IS NOT NULL;
CREATE INDEX idx_poll_votes_poll_id ON poll_votes(poll_id);

-- ────────────────────────────────────────────────────────────
-- 10. NOTIFICATIONS
-- ────────────────────────────────────────────────────────────
CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    body        TEXT         NOT NULL,
    type        VARCHAR(30)  NOT NULL
                  CHECK (type IN (
                      'ISSUE_STATUS_CHANGED','ISSUE_ASSIGNED',
                      'ISSUE_COMMENT','ISSUE_UPVOTED','POLL_CREATED',
                      'POLL_CLOSING_SOON','SLA_BREACH','SYSTEM'
                  )),
    entity_id   UUID,        -- related issue_id or poll_id
    entity_type VARCHAR(20)  CHECK (entity_type IN ('ISSUE','POLL')),
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    read_at     TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id    ON notifications(user_id);
CREATE INDEX idx_notifications_is_read    ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- ────────────────────────────────────────────────────────────
-- 11. AUDIT LOGS  (immutable, append-only)
-- ────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id     UUID         REFERENCES users(id) ON DELETE SET NULL,
    actor_email  VARCHAR(255),
    action       VARCHAR(100) NOT NULL,   -- e.g. ISSUE_CREATED, STATUS_CHANGED
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    UUID,
    old_value    JSONB,
    new_value    JSONB,
    ip_address   VARCHAR(45),
    user_agent   VARCHAR(500),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_actor     ON audit_logs(actor_id);
CREATE INDEX idx_audit_entity    ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created   ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_action    ON audit_logs(action);

-- ────────────────────────────────────────────────────────────
-- 12. WEBHOOKS  (for authority external system integration)
-- ────────────────────────────────────────────────────────────
CREATE TABLE webhooks (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL,
    url         VARCHAR(500) NOT NULL,
    secret      VARCHAR(255) NOT NULL,
    events      TEXT[]       NOT NULL,   -- array of event types to subscribe to
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    owner_id    UUID    NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
