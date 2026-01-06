-- Sample data for Veld Spring Boot Example
-- This file will be executed on startup to populate the database

-- Insert sample users
INSERT INTO users (username, email, first_name, last_name, created_at, active) VALUES 
('johndoe', 'john.doe@example.com', 'John', 'Doe', CURRENT_TIMESTAMP, true),
('janedoe', 'jane.doe@example.com', 'Jane', 'Doe', CURRENT_TIMESTAMP, true),
('bobsmith', 'bob.smith@example.com', 'Bob', 'Smith', CURRENT_TIMESTAMP, true),
('alicejohnson', 'alice.johnson@example.com', 'Alice', 'Johnson', CURRENT_TIMESTAMP, false);

-- Get user IDs for todos (using subquery)
INSERT INTO todos (title, description, completed, created_at, priority, user_id) 
SELECT 'Learn Veld Framework', 'Study Veld DI framework and its Spring Boot integration', false, CURRENT_TIMESTAMP, 'HIGH', id FROM users WHERE username = 'johndoe'
UNION ALL
SELECT 'Complete Spring Boot Migration', 'Migrate existing Spring services to use Veld components', false, CURRENT_TIMESTAMP, 'MEDIUM', id FROM users WHERE username = 'johndoe'
UNION ALL
SELECT 'Review Documentation', 'Update and improve Veld documentation with Spring Boot examples', false, CURRENT_TIMESTAMP, 'LOW', id FROM users WHERE username = 'johndoe'
UNION ALL
SELECT 'Write Unit Tests', 'Create comprehensive test suite for Veld components', true, CURRENT_TIMESTAMP, 'HIGH', id FROM users WHERE username = 'janedoe'
UNION ALL
SELECT 'Setup CI/CD Pipeline', 'Configure GitHub Actions for automated testing and deployment', true, CURRENT_TIMESTAMP, 'MEDIUM', id FROM users WHERE username = 'janedoe'
UNION ALL
SELECT 'Code Review', 'Review and approve pull requests from team members', false, CURRENT_TIMESTAMP, 'LOW', id FROM users WHERE username = 'bobsmith'
UNION ALL
SELECT 'Performance Optimization', 'Optimize Veld component initialization and injection', true, CURRENT_TIMESTAMP, 'HIGH', id FROM users WHERE username = 'alicejohnson'
UNION ALL
SELECT 'Security Audit', 'Perform security review of Spring Boot integration', false, CURRENT_TIMESTAMP, 'HIGH', id FROM users WHERE username = 'alicejohnson';
