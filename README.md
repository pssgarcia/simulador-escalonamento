# Simulador de Escalonamento de CPU
**Disciplina:** Sistemas Operacionais — PUC Minas 2026/1
**Trabalho Prático 1**

---

## Estrutura do Projeto

```
simulador-escalonamento/
├── src/
│   └── escalonador/
│       ├── Process.java          # Dados estáticos + estado dinâmico de cada processo
│       ├── Metrics.java          # Cálculo de espera média, turnaround e throughput
│       ├── Scheduler.java        # Interface comum a todos os algoritmos (padrão Strategy)
│       ├── FileParser.java       # Leitura e parse do processos.txt
│       ├── Main.java             # Ponto de entrada: itera algoritmos e imprime tabela
│       └── algoritmos/
│           ├── FCFS.java         # First-Come, First-Served
│           ├── SRTF.java         # Shortest Remaining Time First
│           ├── RoundRobin.java   # Round-Robin com Média Exponencial
│           └── MLQ.java          # Multilevel Queue (RR + FCFS)
├── processos.txt                 # Arquivo de entrada padrão (100 processos)
└── README.md
```

---

## Compilação

A partir da raiz do projeto:

```bash
# Criar diretório de saída (apenas na primeira vez)
mkdir out

# Compilar
javac -d out -sourcepath . src/escalonador/Main.java
```

---

## Execução

```bash
# Usar o processos.txt padrão (na raiz do projeto)
java -cp out src.escalonador.Main

# Especificar outro arquivo de entrada
java -cp out src.escalonador.Main caminho/outro_arquivo.txt
```

---

## Formato do Arquivo de Entrada

```
PID;Chegada;BurstTotal;Prioridade;[IO1,IO2,...]
```

| Campo        | Descrição                                                  |
|--------------|------------------------------------------------------------|
| PID          | Identificador do processo                                  |
| Chegada      | Tempo de chegada na fila de prontos                        |
| BurstTotal   | Total de tempo de CPU necessário (ms)                      |
| Prioridade   | 1 = alta (Fila 1 no MLQ), 2+ = baixa (Fila 2 no MLQ)     |
| Instantes IO | CPU acumulada em que ocorre cada I/O (cada um dura 5 ut)  |

**Exemplo:**
```
101;0;40;2;10,25
```
- P101 chega em t=0, precisa de 40ms de CPU, prioridade 2
- Faz I/O após 10ms acumulados de CPU, e outro após 25ms acumulados

---

## Algoritmos Implementados

| Algoritmo | Tipo | Decisão de escalonamento |
|---|---|---|
| FCFS (First-Come, First-Served) | Não-preemptivo | Ordem de chegada; nunca interrompe |
| SRTF (Shortest Remaining Time First) | Preemptivo | Menor `remainingBurst`; preempta se chegar alguém menor |
| Round-Robin com Predição (α=0.5, τ₀=10ms) | Preemptivo | Fila circular; quantum = menor τ na fila |
| MLQ (RR quantum=4 + FCFS) | Preemptivo entre filas | Fila 1 (prio=1) tem prioridade absoluta sobre Fila 2 |

---

## Decisões de Projeto

### Padrão Strategy
A interface `Scheduler` define o contrato `run(List<Process>) → Metrics`. O `Main` não sabe qual algoritmo está rodando — apenas itera a lista e chama `run()`. Adicionar um novo algoritmo é criar uma classe e registrá-la em `Main.java`.

### `Process.copy()`
Cada algoritmo parte de um estado zerado. Sem `copy()`, o `remainingBurst`, `accumulatedCpu` e `tau` deixados pelo FCFS contaminariam o SRTF. O `copy()` cria uma instância nova com todos os campos dinâmicos resetados.

### MLQ — `ArrayDeque` na Fila 2
Quando a Fila 1 preempta um processo da Fila 2, ele precisa voltar ao *início* da fila (para preservar a ordem FCFS). `ArrayDeque` oferece `addFirst()` para isso; uma `Queue` comum só tem `add()`, que coloca no fim.

### Round-Robin — Quantum dinâmico
O quantum é recalculado a cada troca de contexto como o menor τ entre os processos na fila de prontos mais o processo em execução. O τ é atualizado pela Média Exponencial em três momentos: esgotamento de quantum, bloqueio em I/O e conclusão do processo.

---

## Resultados

Entrada: `processos.txt` — 100 processos, chegadas de t=0 a t=248, bursts de 11 a 55ms, ~50% prio=1, ~50% prio=2.

| Algoritmo | Espera Média (ms) | Turnaround Médio (ms) | Vazão (proc/ut) |
|---|---:|---:|---:|
| FCFS | 1302,73 | 1335,81 | 0,03469 |
| **SRTF** | **967,31** | **1000,39** | **0,03457** |
| Round-Robin com Predição | 1978,39 | 2011,47 | 0,03469 |
| MLQ | 1558,76 | 1591,84 | 0,03469 |

**Análise:** O SRTF obteve a menor espera média (967ms), o que é esperado — SJF/SRTF é provadamente ótimo para minimizar tempo de espera médio. O Round-Robin com Predição teve a maior espera: com τ₀=10ms alto e processos de burst médio (~28ms), o quantum inicial grande favorece processos longos e causa overhead de context switch. O FCFS é previsível mas sofre com o efeito comboio. O MLQ favorece processos de prioridade 1, mas processos de prioridade 2 acumulam espera enquanto a Fila 1 não esvazia.

---

## Integrantes do Grupo

- Pedro Soares de Souza Garcia
- Lucas Guimarães Pós
- Gustavo Pinheiro
- Lucas Alexandre Soares Gomes da Silva
