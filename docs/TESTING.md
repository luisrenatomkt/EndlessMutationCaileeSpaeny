# Testes e validação

Legenda: ✅ executado e aprovado · ⬜ requer Minecraft (procedimento abaixo, **ainda não executado**).

## Automatizados (`./gradlew selfTest`) — ✅ 29/29 executados com javac 8
- SciNumber: rejeita NaN/∞/negativo; soma 1e300+1e300; ordenação.
- Sem crescimento por tempo/fome (energia, massa, nível inalterados após 100 mil ticks de fome).
- Concessão única: 2ª chamada com o mesmo UUID não concede; energia inválida rejeitada; refeição reduz a fome.
- Persistência (restore): preserva tudo, inclusive idempotência; dados ausentes → padrões seguros.
- Monotonia/finitude de g e nível em 2000 refeições.
- Valores extremos: energia > Double.MAX_VALUE (≈1e311) continua finita; escalas finitas e dentro do limite visual; progressão lógica continua (espinhos lógicos > 500) quando o visual satura.
- Multiplicadores finitos/limitados; entrada NaN → escala 1.0; sem alimentação → escala 1.0.

## Dentro do Minecraft (⬜ não executados)
Prepare: mundo criativo plano, `enableDebugCommands=true` só para o teste 13.
1. **Inicialização:** o jogo abre com o mod listado (Mods → Endless Mutation 1.0.0); sem crash no log.
2. **Registro:** ovo "Predadora Mutante" no criativo; item "Amuleto da Predadora" com ícone rosa.
3. **Invocação:** ovo, amuleto e `/endlessmutation summon` criam a NPC; ela é visível (vestido rosa, cabelo castanho) com nome e Lv.
4. **Localização:** solte 3 vacas a ~15 blocos; ela deve se mover até uma (com `requirePath=true`, não escolhe animais cercados por paredes).
5. **Perseguição/combate:** ela corre, para em alcance, ataca em intervalos (cooldown) e a vaca morre.
6. **Consumo:** ao morrer, anima alimentação (cabeça baixa, partículas) por `feedTicks`; **sem drops** (`consumeDrops=true`).
7. **Concessão única:** `/endlessmutation info` antes/depois: Refeições +1, Energia += valor da vaca (10). Mate uma vaca com **outra** causa (lava): Refeições não muda.
8. **Crescimento:** após ~10-20 vacas, braços/pernas/peito visivelmente mais grossos; espinhos nos ombros aparecem.
9. **Sem alimentação:** deixe-a 10 min sem presas (ou `searchRadius` mínimo): `info` mostra g e energia constantes; modelo não muda.
10. **Atributos:** `info` mostra Dano crescendo; o cooldown entre golpes diminui.
11. **Imortalidade:** espada de criativo, arco, TNT, lava, fogo, afogamento, queda do void, poção de dano instantâneo, `/kill @e[type=...]`: ela não morre nem some; vida permanece cheia.
12. **Persistência:** evolua, saia e reentre no mundo: mesmo Lv./tamanho/refeições; reiniciar durante perseguição não gera recompensa dupla.
13. **Extremos:** `/endlessmutation debugfeed 100000 1e200` → `info` sem NaN/∞; jogo não trava; modelo limitado a `maxVisualScale`.
14. **CI limpo:** o workflow do GitHub Actions termina verde e publica o JAR.
