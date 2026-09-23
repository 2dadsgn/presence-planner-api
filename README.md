# Presence Planner API

Backend for the Presence Planner Angular app: stores every user's
day-by-day presence assignments centrally (so it's shared across users,
unlike the frontend's original `localStorage`-only mode) and enforces each
team's **minimum office days per week** policy, with per-person exceptions.

> **Built without network access to Maven Central**, so it has not been
> compiled or run in the environment that generated it. The code follows
> standard Spring Boot 3.4 / Spring Security 6 APIs throughout, but please
> run `mvn clean verify` as your first step and treat any compiler error as
> a real bug to report back, not a config issue on your end.

## Stack

Java 21, Spring Boot 3.4, Spring Data JPA, Spring Security (OAuth2 resource
server for Entra ID), Flyway. H2 (file-based) for local dev, PostgreSQL for
a real deployment — same schema, same code, just a Spring profile switch.

## Data model

- **User** — provisioned automatically the first time someone calls the
  API (from their Microsoft token's email/name, or a mock header in dev).
- **Team** — has one manager (a `User`) and a `minOfficeDaysPerWeek` policy.
  A user's `team_id` says which team they're a *member* of; a team's
  `manager_id` says who manages it.
- **PresenceEntry** — one user, one date, one value (`office` / `remote` /
  `vacation` / `sick` / `off`) — matches the frontend's value list exactly.
- **PresenceException** — a manager-created override of the team's minimum
  for one person over a date range (or open-ended). A null
  `minOfficeDaysPerWeek` means "fully exempt" for that period.

## Running locally

```bash
mvn spring-boot:run
```

Runs with the **`dev` profile** by default: an H2 database file under
`./data/`, auto-created via Flyway with a small seed dataset (a manager,
two reports, a team policy of 2 office days/week) — see
`src/main/resources/db/dev-migration/V2__dev_seed_data.sql`.

Auth in dev mode is a **mock header** instead of real Microsoft sign-in
(mirrors the Angular app's own `useMockAuth` flag): every request is
treated as `daniele.lubrano@intecsengineering.it` unless you send
`X-Debug-User-Email` (and optionally `X-Debug-User-Name`) yourself, e.g.:

```bash
curl -H "X-Debug-User-Email: anna.rossi@intecsengineering.it" \
     "http://localhost:8080/api/presence?year=2026&month=9"
```

Try the seeded manager's team view:

```bash
curl "http://localhost:8080/api/teams/mine"
curl "http://localhost:8080/api/teams/1/presence?year=2026&month=9"
```

H2's web console is at `http://localhost:8080/h2-console` (JDBC URL
`jdbc:h2:file:./data/presence-planner`, user `sa`, empty password).

## API overview

| Method | Path | Who | What |
|---|---|---|---|
| GET | `/api/me` | anyone signed in | your profile, your team (if a member), the team you manage (if a manager) |
| GET | `/api/presence?year=&month=` | anyone | your own month's assignments |
| POST | `/api/presence/assign` | anyone | `{dates:[...], type}` — apply a value to days |
| POST | `/api/presence/clear` | anyone | `{dates:[...]}` — remove the value from days |
| GET | `/api/teams/mine` | manager | the team you manage |
| GET | `/api/teams/{id}/members` | that team's manager | its members |
| GET | `/api/teams/{id}/presence?year=&month=` | that team's manager | every member's month **plus weekly compliance** against the policy |
| PUT | `/api/teams/{id}/policy` | that team's manager | `{minOfficeDaysPerWeek}` |
| GET | `/api/teams/{id}/exceptions` | that team's manager | current exceptions |
| POST | `/api/teams/{id}/exceptions` | that team's manager | `{userId, minOfficeDaysPerWeek, startDate, endDate, reason}` |
| DELETE | `/api/teams/{id}/exceptions/{exceptionId}` | that team's manager | remove one |

Every team endpoint re-checks the caller actually manages that team
(`TeamService.assertManagesTeam`) — a manager can never see or edit a team
that isn't theirs, and a non-manager gets a 403 from all of them.

### Compliance

`GET /api/teams/{id}/presence` is the one that answers "who's below the
minimum" — for each member and each Monday–Sunday week touching the
requested month, it returns:

```json
{ "weekStart": "2026-09-07", "weekEnd": "2026-09-13",
  "officeDays": 1, "requiredMinimum": 2, "exempt": false, "compliant": false }
```

`requiredMinimum` already accounts for any active exception for that
person/week (an exception with `minOfficeDaysPerWeek: null` shows
`exempt: true` and is always `compliant`); otherwise it falls back to the
team's policy. The frontend can render this straight into a compliance
column without recomputing anything.

## Connecting real Microsoft/Entra ID sign-in

The dev mock auth is a placeholder for exactly the same reason the
frontend's `useMockAuth` is: so the app runs before Azure AD is set up.
To switch to real sign-in:

1. Register a **second** app in Microsoft Entra ID for this API (distinct
   from the Angular SPA's registration) — this gives you the API's own
   Application (client) ID, used as the token audience.
2. Expose a scope on it (e.g. `access_as_user`) and, in the SPA's app
   registration, grant it permission to request that scope — then add that
   scope to the Angular app's `environment.apiScopes` alongside/in place of
   `User.Read`, so MSAL requests a token meant for *this* API, not just
   Microsoft Graph.
3. Run with the `azuread` profile and set:
   - `AZURE_AD_ISSUER_URI` — `https://login.microsoftonline.com/<tenant-id>/v2.0` (same tenant as the frontend)
   - `AZURE_AD_API_AUDIENCE` — the API app registration's client ID
   - `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` — your PostgreSQL instance (see `docker-compose.yml` for a local one)
   - `CORS_ALLOWED_ORIGINS` — your deployed Angular app's origin

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=azuread
   ```

No code changes needed — `AzureAdSecurityConfig` / `AzureAdJwtDecoderConfig`
take over from the dev mock filter automatically, validating the token's
signature, issuer **and** audience.

## Tests

```bash
mvn test
```

`ComplianceServiceTest` covers the policy logic directly (team minimum,
full exemption, reduced per-person minimum, an exception outside its date
range, week boundaries spanning month edges) without needing a database.
