-- Add stored generated columns that extract text from the JSON data column
ALTER TABLE properties_as_json
    ADD COLUMN title       VARCHAR(255) AS (JSON_VALUE(data, '$.title')) STORED,
    ADD COLUMN description TEXT          AS (JSON_VALUE(data, '$.description')) STORED,
    ADD COLUMN city        VARCHAR(100) AS (JSON_VALUE(data, '$.city')) STORED;

-- Create a FULLTEXT index on the generated columns for native MariaDB fulltext search
CREATE FULLTEXT INDEX ft_properties_json
    ON properties_as_json (title, description, city);

