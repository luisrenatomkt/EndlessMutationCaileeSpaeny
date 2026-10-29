# Endless Mutation

Mod para **Minecraft Java 1.8.9** com **Forge 11.15.1.2318** (Java 8). Adiciona a **Predadora Mutante**: uma NPC
independente, **imortal**, que caça animais, e **só evolui ao devorar** uma presa válida. A evolução **não tem nível máximo**.

> **A personagem é uma criação original e fictícia.** A imagem de referência foi usada apenas como inspiração
> de estilo (cabelo castanho longo com franja, vestido tomara-que-caia rosa-pink). O mod **não** reproduz o rosto
> nem o nome de nenhuma pessoa real. Modelo 3D e texturas são originais, gerados por código neste repositório.

## Estado honesto do projeto

| Item | Estado |
|---|---|
| Código-fonte completo (21 classes Java + recursos) | Escrito |
| Testes matemáticos (`EvolutionSelfTest`, 29 verificações) | **Executados e aprovados** (javac 8 + java) |
| Sintaxe de todo o código | Verificada (0 erros de sintaxe) |
| Compilação contra Forge 1.8.9 / geração do JAR | **NÃO executada** (o ambiente onde o código foi escrito não alcança `maven.minecraftforge.net`). O JAR é gerado pelo GitHub Actions. |
| Testes dentro do Minecraft (caça, combate, imortalidade, NBT) | **NÃO executados** — veja `docs/TESTING.md` |

Como as classes do Minecraft não puderam ser verificadas por um compilador, **a primeira execução do GitHub Actions
pode apontar erros de nomes/assinaturas da API 1.8.9**. Se acontecer, envie o log (`build-logs` no artefato) e ajusta-se.

## Requisitos
- Minecraft Java Edition **1.8.9** + Forge **11.15.1.2318** (instalador da 1.8.9).
- Java 8 para compilar (o Actions já instala).

## Instalação do JAR
1. Obtenha `EndlessMutation-1.0.0.jar` (veja "GitHub Actions" abaixo).
2. Coloque em `.minecraft/mods/` (perfil Forge 1.8.9) e inicie o jogo.

## Como obter o JAR (GitHub Actions)
1. Crie um repositório no GitHub e envie **todo** este diretório (inclui `.github/workflows/build.yml`, `gradlew`, `gradle/wrapper/`).
2. Aba **Actions** → workflow **Build mod JAR** (roda a cada push; ou "Run workflow").
3. Ao concluir com sucesso, abra a execução → seção **Artifacts** → baixe **EndlessMutation-jar**.
4. Em caso de falha, baixe **build-logs** (`setup.log`, `selftest.log`, `build.log`).

O workflow: faz checkout → Java 8 (Temurin) → `setupCIWorkspace` → `selfTest` → `build` → valida que o JAR existe, não está vazio e contém
a classe principal e `mcmod.info` → publica o artefato. Qualquer falha interrompe a execução.

**Risco conhecido (ForgeGradle 2.1 é legado):** depende de repositórios antigos (`maven.minecraftforge.net`, downloads de
`launcher.mojang.com`/`libraries.minecraft.net`). Se algum ficar indisponível o build falha com erro claro no log — não é simulado nada.

## Compilar localmente
```
./gradlew setupDecompWorkspace   # uma vez (demorado)
./gradlew selfTest               # testes matematicos
./gradlew build                  # JAR em build/libs/
./gradlew runClient              # abre o Minecraft de desenvolvimento
```
Requer JDK 8 (`JAVA_HOME`). O wrapper baixa o Gradle 2.14.1.

## Como invocar a NPC
- **Ovo de invocação** (aba "Ovos de geração" do criativo): "Predadora Mutante" (rosa/marrom).
- **Amuleto da Predadora** (aba Diversos): clique direito num bloco.
- **Comando** (OP nível 2): `/endlessmutation summon` · `info` · `remove` · `debugfeed [refeições] [energia]` (este último só com `enableDebugCommands=true`).
Ela aparece com o nome `Predadora Mutante Lv. N` sobre a cabeça. Como é imortal, use `/endlessmutation remove` para removê-la.

## Como funciona a caça
- Busca por **área** (AABB) no raio `searchRadius`, a cada `searchInterval` ticks (reduzido pela fome) — nunca varre o mundo inteiro por tick.
- Presas válidas: tudo que herda de `EntityAnimal` (vaca, vaca-cogumelo, ovelha, porco, galinha, coelho, cavalo/burro/mula, lobo, jaguatirica) +
  morcego e lula, mais nomes extras da config. **Jogadores, outras Predadoras e mobs hostis nunca são alvo.** Lobos domesticados são presas (configurável).
- Pontuação = distância ÷ prioridade (`preyPriority`). Só os 5 melhores candidatos têm o caminho calculado; alvos sem caminho viável vão para uma lista negra de 20 s.
- Perseguição com detecção de "preso" (sem progresso por 4 s → abandona e escolhe outro).
- Ataque nativo (`attackEntityFrom` com `DamageSource.causeMobDamage`), com alcance, linha de visão e cooldown.

## Como funciona a evolução
- **Regra única:** `EvolutionData.consumeOnce(UUID, energia, fatorMassa)` — é o **único** lugar que altera energia/massa. Exige: presa morta **e** golpe
  desta NPC nos últimos 10 s **e** UUID nunca consumido (os últimos 64 UUIDs são persistidos → sem recompensa duplicada ao recarregar chunk/mundo).
- Tempo, fome e distância **não** alteram energia, massa nem nível (verificado por teste).
- Números: energia e massa usam `SciNumber` (mantissa double + expoente long): **sem overflow, NaN ou infinito**, sem BigDecimal por tick.
- Nível = `(E / E0)^(1/p)` (E0=10, p=1.6 por padrão). Índice de crescimento `g = ln(1 + massa/M0)`.
- Por região (14: cabeça, pescoço, trapézio, ombros, peito, costas, braço, antebraço, mão, tronco, quadris, coxa, panturrilha, pé):
  `escala = 1 + peso × gene × intensidade × g^0.9`, com **gene** individual por semente (cada NPC cresce de forma levemente diferente).
- Efeitos: dano, velocidade (teto configurável), cooldown de ataque menor, espinhos ósseos nos ombros, hitbox/altura maiores.
- O modelo é articulado (braço→antebraço→mão, coxa→panturrilha→pé) com matrizes GL; **nenhum objeto de modelo é recriado em runtime**, e o cliente interpola suavemente o crescimento.

## Imortalidade
Camadas: `attackEntityFrom` sempre `false`; `isEntityInvulnerable`; `onDeath` vazio; `setHealth` sempre vida máxima; eventos Forge
`LivingAttack/Hurt/Death` cancelados **só para esta entidade**; imune a fogo/lava; sem dano de queda; respira debaixo d'água; poções ignoradas;
sem despawn; `setDead()` ignorado exceto pelo comando `remove`; queda no void → teleporta ao spawn. O mundo e outras entidades não são afetados.

## Configuração
`config/endlessmutation.cfg` (gerado na primeira execução). Todos os valores têm faixa validada. **Não existe nível máximo.** Os únicos tetos são de
renderização/segurança: `maxVisualScale`, `maxEntityScale`, `maxVisibleSpikes`, `maxSpeedMultiplier`, `maxAttackDamage`.

## Limites reais (leia)
O nível **não tem teto lógico**: energia/massa continuam acumulando em notação científica com expoente de 64 bits. Mas:
- a **exibição** do nível é um texto em notação científica; conversões para `double` são saturadas só para mostrar;
- o **visual** é limitado (`maxVisualScale`) — depois disso a NPC continua evoluindo (energia, dano até `maxAttackDamage`, contagem lógica de espinhos), mas a malha para de crescer;
- o dano é `float` no Minecraft 1.8.9 (por isso o teto finito de dano) e a velocidade tem teto para não quebrar o movimento/pathfinding;
- a hitbox crescente pode prendê-la em locais apertados; entidades muito grandes custam mais pathfinding;
- o jogo, a RAM e a CPU limitam quantas presas existem para serem devoradas.
- Limitação de modelo: é um modelo de caixas (estilo Minecraft) — um "rosto detalhado" é limitado a 8×8 px; não há malha 3D externa nem skin de terceiros.
- Escala por região acontece em torno das articulações; em escalas muito altas, peças vizinhas podem se atravessar visualmente.

## Solução de problemas
- **Build falha ao baixar dependências:** repositório legado indisponível; veja `setup.log`.
- **Crash ao iniciar:** confirme Forge 11.15.1.2318 e Java 8.
- **A NPC não caça:** há animais num raio de `searchRadius` com caminho viável? Aumente o raio; veja `/endlessmutation info`.
- **Textura rosa/preta:** o recurso `assets/endlessmutation/textures/entity/huntress.png` não foi empacotado; rode `python3 tools/generate_textures.py`.
- **Não consigo matá-la / removê-la:** é intencional; use `/endlessmutation remove`.

## Arquitetura
`EndlessMutation` (main) · `proxy/{Common,Client}Proxy` · `entity/{EntityHuntress, PreyRules}` · `ai/{AIHuntAnimals, AIAttackPrey, AIEatPrey}` ·
`evolution/{SciNumber, EvolutionData, MutationSystem}` (puro Java, testável) · `persistence/EvolutionNBT` · `client/{ModelHuntress, RenderHuntress}` ·
`init/{ModEntities, ModItems}` · `item/ItemSummoner` · `event/ImmortalityHandler` · `command/CommandEndless` · `config/ModConfig`.

Licença: MIT (`LICENSE`).
