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

## Srdce
Za zabití hráče dostane zabíjející item **Srdce** (beetroot, nejde sníst). Pravý klik se srdcem v ruce přidá 1 srdce (max `life.max-hp`). Styl LifeSteal: zabitý hráč přijde o jedno srdce (nikdy pod 1 srdce) a zabiják ho dostane jako item. Při smrti navíc dropne 30 % srdcí nad 10. Vše v sekcích `heart-item` a `life`.

## Recepty
Ender chest (4× obsidian + eye of ender), shulker box (4× diamant + chest), enchanted golden apple (4× gold block + netherite ingot) – vše do plusu.
