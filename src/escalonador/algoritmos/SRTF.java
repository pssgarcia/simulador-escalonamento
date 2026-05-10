package src.escalonador.algoritmos;

import src.escalonador.Metrics;
import src.escalonador.Process;
import src.escalonador.Scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SRTF — Shortest Remaining Time First
 *
 * Variante preemptiva do SJF: a cada tick, o escalonador sempre escolhe
 * o processo com o MENOR tempo de CPU restante (remainingBurst).
 * Se um processo novo chegar com burst menor que o atual, ele preempta
 * imediatamente.
 *
 * Fluxo por tick:
 *   1. Processos bloqueados em I/O que atingiram ioReturnTime voltam
 *      para a fila de prontos.
 *   2. Processos que chegaram no tempo atual entram na fila de prontos.
 *   3. Preempção: se algum processo da fila tem remainingBurst menor
 *      que o atual, o atual volta para a fila e o menor assume a CPU.
 *   4. Se a CPU estiver livre, escalona o processo com menor remainingBurst
 *      (desempate por PID).
 *   5. O processo em execução consome 1 unidade de CPU.
 *   6. Se atingiu um instante de I/O, bloqueia; se não tem mais burst, conclui.
 */
public class SRTF implements Scheduler {

    @Override
    public String getName() {
        return "SRTF (Shortest Remaining Time First)";
    }

    @Override
    public Metrics run(List<Process> processes) {

        // Fila de prontos: processos aguardando CPU
        List<Process> readyQueue = new ArrayList<>();

        // Processos concluídos (para cálculo de métricas)
        List<Process> completed = new ArrayList<>();

        // Processos que ainda não chegaram (ordenados por arrivalTime)
        List<Process> notArrived = new ArrayList<>(processes);
        notArrived.sort(Comparator.comparingInt(Process::getArrivalTime));

        // Processos bloqueados em I/O
        List<Process> blocked = new ArrayList<>();

        Process running = null; // processo atualmente na CPU
        int currentTime = 0;

        while (completed.size() < processes.size()) {

            // ---------------------------------------------------------------
            // 1. Libera processos que retornam do I/O neste tick
            // ---------------------------------------------------------------
            for (Process p : new ArrayList<>(blocked)) {
                if (p.ioReturnTime <= currentTime) {
                    p.finishIO();
                    readyQueue.add(p);
                    blocked.remove(p);
                }
            }

            // ---------------------------------------------------------------
            // 2. Admite processos que chegam neste tick
            // ---------------------------------------------------------------
            for (Process p : new ArrayList<>(notArrived)) {
                if (p.getArrivalTime() <= currentTime) {
                    readyQueue.add(p);
                    notArrived.remove(p);
                }
            }

            // ---------------------------------------------------------------
            // 3. Preempção: verifica se algum da fila tem burst menor que o atual
            // ---------------------------------------------------------------
            if (running != null && !readyQueue.isEmpty()) {
                Process shortest = readyQueue.stream()
                        .min(Comparator.comparingInt((Process p) -> p.remainingBurst)
                                .thenComparingInt(Process::getPid))
                        .orElse(null);

                if (shortest != null && shortest.remainingBurst < running.remainingBurst) {
                    readyQueue.add(running);     // devolve o atual para a fila
                    running = shortest;          // o mais curto assume a CPU
                    readyQueue.remove(shortest);
                }
            }

            // ---------------------------------------------------------------
            // 4. Se CPU livre, escalona o processo com menor burst restante
            // ---------------------------------------------------------------
            if (running == null && !readyQueue.isEmpty()) {
                running = readyQueue.stream()
                        .min(Comparator.comparingInt((Process p) -> p.remainingBurst)
                                .thenComparingInt(Process::getPid))
                        .orElse(null);
                readyQueue.remove(running);
            }

            // ---------------------------------------------------------------
            // 5. Executa 1 unidade de CPU
            // ---------------------------------------------------------------
            if (running != null) {
                running.remainingBurst--;
                running.accumulatedCpu++;

                // -----------------------------------------------------------
                // 5a. Verifica se atingiu um instante de I/O
                // -----------------------------------------------------------
                if (running.nextIoIndex < running.getIoInstants().size()
                        && running.accumulatedCpu == running.getIoInstants().get(running.nextIoIndex)) {

                    running.triggerIO(currentTime + 1);
                    blocked.add(running);
                    running = null;

                // -----------------------------------------------------------
                // 5b. Verifica se terminou todo o burst
                // -----------------------------------------------------------
                } else if (running.remainingBurst == 0) {
                    running.completionTime = currentTime + 1;
                    completed.add(running);
                    running = null;
                }
            }

            currentTime++;

            // Guarda contra loop infinito: ninguém em lugar nenhum
            if (running == null && readyQueue.isEmpty()
                    && blocked.isEmpty() && notArrived.isEmpty()
                    && completed.size() < processes.size()) {
                break;
            }
        }

        return Metrics.calculate(getName(), completed, currentTime);
    }
}