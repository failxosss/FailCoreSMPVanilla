# FailCoreSMPVanilla

Plugin pro Paper **1.21.11** (Java 21) – autor **Failxos**.

## Build
- GitHub: nahraj složku na GitHub → záložka **Actions** → *Build* → stáhni artifact `FailCoreSMPVanilla` (jar).
- Lokálně: `mvn clean package` → `target/FailCoreSMPVanilla-1.0.5.jar`

## Příkazy
- `/failcoresmpvanilla [info|reload]` (permission `failcoresmpvanilla.admin`)
- `/rules`

## Permission
- `failcoresmpvanilla.bypass` – obejde omezení kromě LifeSteal (defaultně nikdo, ani OP)

Vše se nastavuje v `plugins/FailCoreSMPVanilla/config.yml`.

## Vlastní recepty
V `config.yml` v sekci `custom-recipes` si můžeš přidat vlastní craftingy (tvarové i bez tvaru), nebo smazat vanilla recepty přes `remove-vanilla-recipes`. Příklady jsou přímo v configu. Načtou se po restartu nebo po `/failcoresmpvanilla reload`.

## Srdce
Za zabití hráče dostane zabíjející item **Srdce** (beetroot, nejde sníst). Pravý klik se srdcem v ruce přidá 1 srdce (max 30 srdcí ve výchozím nastavení (`life.max-hp`)). Styl LifeSteal: zabitý hráč přijde o jedno srdce (nikdy pod 1 srdce) a zabiják ho dostane jako item. Při opakovaném zabití stejného hráče stejným zabijákem platí cooldown 30 sekund od posledního úspěšného převodu srdce (`life.same-victim-cooldown-seconds`). Během cooldownu se srdce neodebere ani nevydá, další zabití čas neprodlužuje. Cooldown je uložen v paměti a restart serveru jej vynuluje. Při jiné smrti se srdce neodebírá ani nedropuje. Na minimu se item nevytváří. LifeSteal platí i pro hráče s bypass oprávněním. Staré klíče victim-loses-hp, steal-only-if-victim-has-hearts a death-drop-* se již nepoužívají. Vše v sekcích `heart-item` a `life`.

## Recepty
Ender chest (4× obsidian + eye of ender), shulker box (4× diamant + chest), enchanted golden apple (4× gold block + netherite ingot) – vše do plusu.

### /withdrawmoney
Vybere jedno vlastni maximalni srdce jako item do hlavni ruky. Minimalne jedno srdce zustane. Pri plnem inventari a obsazene ruce se vyber neprovede. V combatu plati stavajici blokovani prikazu.

Prikaz /withdrawmoney nevyzaduje zadne opravneni.
