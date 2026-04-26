# Simulador de Escalonamento de CPU
**Disciplina:** Sistemas Operacionais — PUC Minas 2026/1
**Trabalho Prático 1**

---

## Estrutura do Projeto

```
cpu-scheduler/
├── src/
│   └── scheduler/
│       ├── Process.java                        # Estrutura de dados do processo
│       ├── Metrics.java                        # Cálculo e exibição de métricas
│       ├── Scheduler.java                      # Interface comum aos algoritmos
│       ├── FileParser.java                     # Leitura do processos.txt
│       ├── Main.java                           # Ponto de entrada
│       └── algorithms/
│           ├── FCFS.java                       # First-Come, First-Served
│           ├── SRTF.java                       # Shortest Remaining Time First
│           ├── RoundRobinPrediction.java       # Round-Robin com Média Exponencial
│           └── MLQ.java                        # Multilevel Queue
├── processos.txt                               # Arquivo de entrada padrão
└── README.md
```

---

## Compilação

A partir da raiz do projeto:

```bash
# Criar diretório de saída
mkdir -p out

# Compilar todos os arquivos Java
javac -d out -sourcepath src src/scheduler/Main.java
```

---

## Execução

```bash
# Usar o processos.txt padrão (na raiz do projeto)
java -cp out scheduler.Main

# Especificar outro arquivo de entrada
java -cp out scheduler.Main caminho/outro_arquivo.txt
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

| Algoritmo | Tipo | Responsável |
|---|---|---|
| FCFS (First-Come, First-Served) | Não-preemptivo | — |
| SRTF (Shortest Remaining Time First) | Preemptivo | — |
| Round-Robin com Predição (α=0.5, τ₀=10ms) | Preemptivo | — |
| MLQ (RR quantum=4 + FCFS) | Preemptivo entre filas | — |

---

## Decisões de Projeto

### MLQ — Critério de distribuição de filas
- **Prioridade 1:** Fila 1 (alta prioridade) — escalonada via Round-Robin
- **Prioridade 2+:** Fila 2 (baixa prioridade) — escalonada via FCFS
- Quantum fixo da Fila 1: **4 unidades de tempo**

### Round-Robin com Predição
- Quantum recalculado a cada troca de contexto
- Quantum = menor τ entre todos os processos na fila de prontos
- τ atualizado pela Média Exponencial após cada surto: `τ_{n+1} = 0.5 * t_n + 0.5 * τ_n`

---

## Resultados

*(Preencher após implementação completa)*

| Algoritmo | Espera Média | Turnaround Médio | Vazão |
|---|---|---|---|
| FCFS | — | — | — |
| SRTF | — | — | — |
| RR com Predição | — | — | — |
| MLQ | — | — | — |

---

## Integrantes do Grupo

- Pedro Soares de Souza Garcia
- 
- 
- 