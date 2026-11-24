# FailCoreSMPVanilla

Plugin pro Paper **1.21.11** (Java 21) – autor **Failxos**.

## Build
- GitHub: nahraj složku na GitHub → záložka **Actions** → *Build* → stáhni artifact `FailCoreSMPVanilla` (jar).
- Lokálně: `mvn clean package` → `target/FailCoreSMPVanilla-1.0.0.jar`

## Příkazy
- `/failcoresmpvanilla [info|reload]` (permission `failcoresmpvanilla.admin`)
- `/rules`

## Permission
- `failcoresmpvanilla.bypass` – obejde všechna omezení (defaultně nikdo, ani OP)

Vše se nastavuje v `plugins/FailCoreSMPVanilla/config.yml`.

## Vlastní recepty
V `config.yml` v sekci `custom-recipes` si můžeš přidat vlastní craftingy (tvarové i bez tvaru), nebo smazat vanilla recepty přes `remove-vanilla-recipes`. Příklady jsou přímo v configu. Načtou se po restartu nebo po `/failcoresmpvanilla reload`.
