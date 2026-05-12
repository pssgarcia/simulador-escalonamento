package src.escalonador.algoritmos;

import src.escalonador.Metrics;
import src.escalonador.Process;
import src.escalonador.Scheduler;

import java.util.*;

/**
 * MLQ — Multilevel Queue (Fila Multinível)
 *
 * Duas filas estáticas com prioridades fixas. Processos NÃO migram entre filas.
 *
 *   Fila 1 (Alta Prioridade) — prioridade == 1 → Round-Robin, quantum fixo = 4
 *   Fila 2 (Baixa Prioridade) — prioridade >= 2 → FCFS
 *
 * Regras:
 *   - Fila 2 só executa quando a Fila 1 está completamente vazia.
 *   - Se um processo da Fila 1 chegar enquanto a Fila 2 está rodando,
 *     o processo da Fila 2 é preemptado e volta ao INÍCIO da Fila 2
 *     (para preservar a ordem FCFS).
 *   - Após I/O, cada processo volta para a sua fila de origem.
 *
 * Fluxo por tick:
 *   1. Processos bloqueados em I/O que retornaram voltam à sua fila original.
 *   2. Processos que chegaram são distribuídos nas filas por prioridade.
 *   3. Preempção entre filas: Fila 2 rodando + Fila 1 não vazia → preempta.
 *   4. Preempção por quantum: processo da Fila 1 esgotou quantum → vai pro fim da Fila 1.
 *   5. Seleciona próximo: Fila 1 tem prioridade absoluta sobre Fila 2.
 *   6. O processo em execução consome 1 unidade de CPU.
 *   7. Se atingiu I/O, bloqueia; se esgotou o burst, conclui.
 */
public class MLQ implements Scheduler {

    /** Quantum fixo da Fila 1 (Round-Robin de alta prioridade). */
    private static final int QUANTUM_FILA1 = 4;

    @Override
    public String getName() {
        return "MLQ (Multilevel Queue — RR + FCFS)";
    }

    /** Critério de distribuição: prioridade 1 → Fila 1; demais → Fila 2. */
    private boolean isAltaPrioridade(Process p) {
        return p.getPriority() == 1;
    }

    @Override
    public Metrics run(List<Process> processes) {

        Queue<Process> fila1 = new LinkedList<>();  // Alta prioridade — Round-Robin
        Deque<Process> fila2 = new ArrayDeque<>();  // Baixa prioridade — FCFS
        //                     ^^ Deque permite addFirst() para preempção sem perder posição

        List<Process> completed  = new ArrayList<>();
        List<Process> blocked    = new ArrayList<>();
        List<Process> notArrived = new ArrayList<>(processes);
        notArrived.sort(Comparator.comparingInt(Process::getArrivalTime));

        Process running = null;
        int currentTime = 0;
        int quantumLeft = 0;

        while (completed.size() < processes.size()) {

            // ---------------------------------------------------------------
            // 1. Libera processos que retornam do I/O — voltam para SUA fila original
            // ---------------------------------------------------------------
            for (Process p : new ArrayList<>(blocked)) {
                if (p.ioReturnTime <= currentTime) {
                    p.finishIO();
                    if (isAltaPrioridade(p)) {
                        fila1.add(p);
                    } else {
                        fila2.addLast(p); // volta ao fim da Fila 2 (FCFS após I/O)
                    }
                    blocked.remove(p);
                }
            }

            // ---------------------------------------------------------------
            // 2. Admite processos que chegam — distribui por prioridade
            // ---------------------------------------------------------------
            for (Process p : new ArrayList<>(notArrived)) {
                if (p.getArrivalTime() <= currentTime) {
                    if (isAltaPrioridade(p)) {
                        fila1.add(p);
                    } else {
                        fila2.addLast(p);
                    }
                    notArrived.remove(p);
                }
            }

            // ---------------------------------------------------------------
            // 3. Preempção entre filas:
            //    Se um processo da Fila 2 está rodando e a Fila 1 ganhou processo,
            //    o processo da Fila 2 volta ao INÍCIO da sua fila (preserva FCFS)
            // ---------------------------------------------------------------
            if (running != null && !isAltaPrioridade(running) && !fila1.isEmpty()) {
                fila2.addFirst(running); // addFirst = devolve à frente da fila
                running = null;
                quantumLeft = 0;
            }

            // ---------------------------------------------------------------
            // 4. Preempção por quantum dentro da Fila 1:
            //    Quantum esgotado → processo volta ao FIM da Fila 1 (Round-Robin)
            // ---------------------------------------------------------------
            if (running != null && isAltaPrioridade(running) && quantumLeft == 0) {
                fila1.add(running); // addLast implícito — fim da fila
                running = null;
            }

            // ---------------------------------------------------------------
            // 5. Seleciona próximo processo (Fila 1 tem prioridade absoluta)
            // ---------------------------------------------------------------
            if (running == null) {
                if (!fila1.isEmpty()) {
                    running = fila1.poll();
                    quantumLeft = QUANTUM_FILA1;
                } else if (!fila2.isEmpty()) {
                    running = fila2.poll();
                    quantumLeft = Integer.MAX_VALUE; // FCFS: sem limite de quantum
                }
            }

            // ---------------------------------------------------------------
            // 6. Executa 1 unidade de CPU
            // ---------------------------------------------------------------
            if (running != null) {
                running.remainingBurst--;
                running.accumulatedCpu++;
                // Só decrementa quantum se for processo da Fila 1 (evita overflow do MAX_VALUE)
                if (isAltaPrioridade(running)) quantumLeft--;

                // ---------------------------------------------------------------
                // 6a. Verifica se atingiu um instante de I/O
                // ---------------------------------------------------------------
                if (running.shouldDoIO()) {
                    running.triggerIO(currentTime + 1);
                    blocked.add(running);
                    running = null;
                    quantumLeft = 0;

                // ---------------------------------------------------------------
                // 6b. Verifica se terminou todo o burst
                // ---------------------------------------------------------------
                } else if (running.remainingBurst == 0) {
                    running.completionTime = currentTime + 1;
                    completed.add(running);
                    running = null;
                    quantumLeft = 0;
                }
            }

            currentTime++;

            // Guarda contra loop infinito
            if (running == null && fila1.isEmpty() && fila2.isEmpty()
                    && blocked.isEmpty() && notArrived.isEmpty()
                    && completed.size() < processes.size()) {
                break;
            }
        }

        return Metrics.calculate(getName(), completed, currentTime);
    }
}