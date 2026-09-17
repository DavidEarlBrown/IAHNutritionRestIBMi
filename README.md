# IAH Nutrition REST for IBM i

Java optimization API plus an **ILE RPG REST data layer**. Java never opens Db2. All reads and writes go to the RPG CGI service (`IAHRSTD`). Linear and nonlinear least-cost formulation stay in Java.

## Architecture

```
HTTP clients
    |
    v
Java  /api/*     (optimize, facade)
    |
    |  JSON HTTP
    v
ILE RPG  /iahdata/*   (IAHRSTD CGI)     local profile uses /data as a simulator
    |
    v
Db2 library IAHNUTR
```

RPG source: `ibmi/` (compile with `CRTIAH`)  
Java source: this Git repo / IFS  
Tables: `src/main/resources/db/schema-ibmi.sql`

## Local run

JDK 17+ and Maven. The `local` profile hosts a `/data` simulator with the same URLs as RPG, and Java still calls it over HTTP.

```bash
mvn spring-boot:run
```

- Public API: http://localhost:8080/api
- Data API (RPG contract): http://localhost:8080/data
- Health: http://localhost:8080/actuator/health

## IBM i

1. Create `IAHNUTR` with `schema-ibmi.sql`.
2. Copy `ibmi/` to the IFS and run `CALL IAHNUTSRC/CRTIAH` (see `ibmi/README.md`).
3. Start Apache with `ibmi/httpd-iahdata.conf`.
4. Build Java: `mvn -DskipTests package`
5. Run the jar in PASE:

```bash
export SPRING_PROFILES_ACTIVE=ibmi
export IAH_DATA_BASE_URL=http://127.0.0.1:10010/iahdata
java -jar /iah/nutrition/iah-nutrition-rest.jar
```

## Optimize

```bash
curl -s -X POST http://localhost:8080/api/optimize \
  -H 'Content-Type: application/json' \
  -d '{
    "clientId": 1,
    "requirementSetId": 1,
    "animalAgeDays": 1200,
    "optimizationType": "LINEAR",
    "saveFormula": true,
    "formulaName": "Demo lactating formula"
  }'
```

Java loads ingredients and requirements from RPG (or `/data`), solves, then `POST /formulas` as one document so RPG can commit header + lines together.

`INGREDNUT` is the dense ingredient × nutrient matrix (one row per pair, amount `0` if unknown). RPG fills missing rows when ingredients or nutrients are created; `GET /api/ingrednut` lists the matrix.
