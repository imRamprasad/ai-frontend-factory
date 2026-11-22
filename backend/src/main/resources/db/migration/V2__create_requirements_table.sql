CREATE TABLE requirements (
    id CHAR(36) PRIMARY KEY,
    project_id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_requirements_project
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_requirements_category CHECK (
        category IN ('FUNCTIONAL', 'NON_FUNCTIONAL', 'TECHNICAL', 'DESIGN', 'SECURITY')
    )
);

CREATE INDEX idx_requirements_project_id ON requirements(project_id);
CREATE INDEX idx_requirements_category ON requirements(category);
CREATE INDEX idx_requirements_created_at ON requirements(created_at);
