# IAH Nutrition — ILE RPG data API

Java does **not** talk to Db2. ILE RPG CGI program `IAHRSTD` is the only reader/writer for library `IAHNUTR`. Java calls these HTTP endpoints, then runs linear/nonlinear optimization in memory.

## Layout

| IFS path | IBM i object | Role |
| --- | --- | --- |
| `ibmi/src/qrpglesrc/*.rpgle` | modules in `IAHNUTSRC` | SQL CRUD + CGI |
| `ibmi/src/qsrvsrc/iahdata.bnd` | `IAHDATA` *SRVPGM | exported procedures |
| `ibmi/src/qrpglesrc/iahrstd.rpgle` | `IAHRSTD` *PGM | REST router |
| `src/main/resources/db/schema-ibmi.sql` | library `IAHNUTR` | tables |

## Build

```
CRTCLPGM PGM(IAHNUTSRC/CRTIAH) SRCSTMF('/iah/nutrition/ibmi/src/qcllesrc/crtiah.clle')
CALL IAHNUTSRC/CRTIAH PARM('/iah/nutrition/ibmi/src')
```

Create tables first with `schema-ibmi.sql` (and optional `seed-data.sql`).

## HTTP Server

Include `ibmi/httpd-iahdata.conf` (port 10010) or add the `ScriptAlias` to an existing Apache instance:

```
ScriptAlias /iahdata /qsys.lib/iahnutsrc.lib/iahrstd.pgm
```

## Contract (same as local Java `/data`)

| Method | Path | Use |
| --- | --- | --- |
| GET/POST | `/iahdata/nutrients` | list / create (create also fills `INGREDNUT` for every ingredient) |
| GET/PUT/DELETE | `/iahdata/nutrients/{id}` | item |
| GET | `/iahdata/ingrednut` | full ingredient × nutrient matrix (`?ingredientId=` optional) |
| GET | `/iahdata/ingredients/{id}/ingrednut` | matrix rows for one ingredient |
| GET | `/iahdata/ingredients?view=full` | catalog with composition |
| POST | `/iahdata/formulas` | save whole formula (one commit) |

Java IBM i profile:

```
export SPRING_PROFILES_ACTIVE=ibmi
export IAH_DATA_BASE_URL=http://127.0.0.1:10010/iahdata
java -jar iah-nutrition-rest.jar
```

RPG source stays in the IFS (or copy members into `IAHNUTSRC/QRPGLESRC` if you prefer traditional source PFs). Data stays in `IAHNUTR`.
