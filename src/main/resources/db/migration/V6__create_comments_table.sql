CREATE TABLE comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    author_id INT NOT NULL,
    notice_id INT NOT NULL,
    parent_id INT NULL,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES employee(employee_id),
    CONSTRAINT fk_comment_notice FOREIGN KEY (notice_id) REFERENCES notice(notice_id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comments(id)
);