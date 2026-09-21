-- Amplia a lista fixa de tecnologias (fontes: Stack Overflow Developer Survey 2025
-- e levantamentos de vagas de TI no Brasil). Nao duplica nomes ja existentes.
CREATE UNIQUE INDEX IF NOT EXISTS uq_technology_name_lower ON technology (lower(name));

INSERT INTO technology (name, category)
SELECT v.name, v.category
FROM (VALUES
    -- Linguagens
    ('HTML', 'Linguagem'), ('CSS', 'Linguagem'), ('Bash', 'Linguagem'), ('C++', 'Linguagem'),
    ('C', 'Linguagem'), ('PowerShell', 'Linguagem'), ('PHP', 'Linguagem'), ('Golang', 'Linguagem'),
    ('Rust', 'Linguagem'), ('Kotlin', 'Linguagem'), ('Lua', 'Linguagem'), ('Ruby', 'Linguagem'),
    ('Dart', 'Linguagem'), ('Swift', 'Linguagem'), ('Groovy', 'Linguagem'), ('Scala', 'Linguagem'),
    ('Elixir', 'Linguagem'), ('Perl', 'Linguagem'), ('Objective-C', 'Linguagem'), ('COBOL', 'Linguagem'),
    ('Haskell', 'Linguagem'), ('Clojure', 'Linguagem'), ('Solidity', 'Linguagem'), ('Julia', 'Linguagem'),
    ('MATLAB', 'Linguagem'), ('Visual Basic', 'Linguagem'), ('Delphi', 'Linguagem'), ('Assembly', 'Linguagem'),
    ('PL/SQL', 'Linguagem'), ('T-SQL', 'Linguagem'), ('Sass', 'Linguagem'),
    -- Frameworks e bibliotecas
    ('Next.js', 'Framework'), ('Express.js', 'Framework'), ('jQuery', 'Framework'), ('ASP.NET Core', 'Framework'),
    ('Vue.js', 'Framework'), ('Nuxt', 'Framework'), ('Svelte', 'Framework'), ('FastAPI', 'Framework'),
    ('Flask', 'Framework'), ('Django', 'Framework'), ('Laravel', 'Framework'), ('WordPress', 'Framework'),
    ('NestJS', 'Framework'), ('Spring', 'Framework'), ('Hibernate', 'Framework'), ('Quarkus', 'Framework'),
    ('Micronaut', 'Framework'), ('Symfony', 'Framework'), ('Ruby on Rails', 'Framework'), ('Flutter', 'Framework'),
    ('React Native', 'Framework'), ('Tailwind CSS', 'Framework'), ('Bootstrap', 'Framework'), ('Blazor', 'Framework'),
    ('Entity Framework', 'Framework'), ('Jakarta EE', 'Framework'), ('Redux', 'Framework'), ('Ionic', 'Framework'),
    ('Electron', 'Framework'), ('Android', 'Framework'), ('iOS', 'Framework'), ('SwiftUI', 'Framework'),
    ('TensorFlow', 'Framework'), ('PyTorch', 'Framework'), ('Pandas', 'Framework'), ('NumPy', 'Framework'),
    ('Scikit-learn', 'Framework'), ('Apache Spark', 'Framework'), ('Selenium', 'Framework'), ('Cypress', 'Framework'),
    ('Jest', 'Framework'), ('JUnit', 'Framework'), ('Mockito', 'Framework'),
    -- Bancos de dados
    ('SQLite', 'Banco de dados'), ('SQL Server', 'Banco de dados'), ('Redis', 'Banco de dados'),
    ('MariaDB', 'Banco de dados'), ('Elasticsearch', 'Banco de dados'), ('Oracle', 'Banco de dados'),
    ('DynamoDB', 'Banco de dados'), ('BigQuery', 'Banco de dados'), ('Supabase', 'Banco de dados'),
    ('Firestore', 'Banco de dados'), ('Firebase', 'Banco de dados'), ('Cosmos DB', 'Banco de dados'),
    ('Snowflake', 'Banco de dados'), ('InfluxDB', 'Banco de dados'), ('Cassandra', 'Banco de dados'),
    ('Neo4j', 'Banco de dados'), ('CouchDB', 'Banco de dados'),
    -- Ferramentas, cloud, DevOps e praticas
    ('Kubernetes', 'Ferramenta'), ('Azure', 'Ferramenta'), ('Google Cloud', 'Ferramenta'), ('Terraform', 'Ferramenta'),
    ('Maven', 'Ferramenta'), ('Gradle', 'Ferramenta'), ('npm', 'Ferramenta'), ('Yarn', 'Ferramenta'),
    ('Webpack', 'Ferramenta'), ('Vite', 'Ferramenta'), ('Jenkins', 'Ferramenta'), ('GitHub', 'Ferramenta'),
    ('GitLab', 'Ferramenta'), ('GitHub Actions', 'Ferramenta'), ('Bitbucket', 'Ferramenta'), ('Jira', 'Ferramenta'),
    ('Linux', 'Ferramenta'), ('Ansible', 'Ferramenta'), ('Kafka', 'Ferramenta'), ('RabbitMQ', 'Ferramenta'),
    ('Nginx', 'Ferramenta'), ('Prometheus', 'Ferramenta'), ('Grafana', 'Ferramenta'), ('Datadog', 'Ferramenta'),
    ('Power BI', 'Ferramenta'), ('Tableau', 'Ferramenta'), ('Postman', 'Ferramenta'), ('Swagger', 'Ferramenta'),
    ('GraphQL', 'Ferramenta'), ('REST', 'Ferramenta'), ('Microservices', 'Ferramenta'), ('CI/CD', 'Ferramenta'),
    ('Cloudflare', 'Ferramenta'), ('Figma', 'Ferramenta'), ('Airflow', 'Ferramenta'), ('Databricks', 'Ferramenta'),
    ('SonarQube', 'Ferramenta'), ('OpenShift', 'Ferramenta'), ('Helm', 'Ferramenta'), ('Argo CD', 'Ferramenta'),
    ('Scrum', 'Ferramenta'), ('Kanban', 'Ferramenta')
) AS v(name, category)
WHERE NOT EXISTS (SELECT 1 FROM technology t WHERE lower(t.name) = lower(v.name));
