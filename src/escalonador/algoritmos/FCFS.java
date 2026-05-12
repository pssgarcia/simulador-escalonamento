package src.escalonador.algoritmos;

import src.escalonador.Metrics;
import src.escalonador.Process;
import src.escalonador.Scheduler;

import java.util.*;

/**
 * FCFS — First-Come, First-Served
 *
 * Algoritmo NÃO-PREEMPTIVO: uma vez na CPU, o processo executa até terminar
 * ou fazer I/O. Nunca é interrompido por um processo que chegou depois.
 *
 * Fluxo por tick:
 *   1. Processos bloqueados em I/O que atingiram ioReturnTime voltam à fila.
 *   2. Processos que chegaram no tempo atual entram na fila de prontos.
 *   3. Se a CPU está livre, o processo que chegou primeiro é selecionado.
 *   4. O processo em execução consome 1 unidade de CPU.
 *   5. Se atingiu um instante de I/O, bloqueia; se esgotou o burst, conclui.
 */
public class FCFS implements Scheduler {

    @Override
    public String getName() {
        return "FCFS (First-Come, First-Served)";
    }

    @Override
    public Metrics run(List<Process> processes) {

        // Fila de prontos: ordenada por arrival, depois PID (desempate)
        List<Process> readyQueue = new ArrayList<>();

        List<Process> completed  = new ArrayList<>();
        List<Process> blocked    = new ArrayList<>();
        List<Process> notArrived = new ArrayList<>(processes);
        notArrived.sort(Comparator.comparingInt(Process::getArrivalTime));

        Process running    = null;
        int currentTime    = 0;

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
            // 3. FCFS não preempta — seleciona somente quando CPU está livre
            // ---------------------------------------------------------------
            if (running == null && !readyQueue.isEmpty()) {
                readyQueue.sort(Comparator.comparingInt(Process::getArrivalTime)
                        .thenComparingInt(Process::getPid));
                running = readyQueue.remove(0);
            }

            // ---------------------------------------------------------------
            // 4. Executa 1 unidade de CPU
            // ---------------------------------------------------------------
            if (running != null) {
                running.remainingBurst--;
                running.accumulatedCpu++;

                // ---------------------------------------------------------------
                // 4a. Verifica se atingiu um instante de I/O
                // ---------------------------------------------------------------
                if (running.shouldDoIO()) {
                    running.triggerIO(currentTime + 1);
                    blocked.add(running);
                    running = null;

                // ---------------------------------------------------------------
                // 4b. Verifica se terminou todo o burst
                // ---------------------------------------------------------------
                } else if (running.remainingBurst == 0) {
                    running.completionTime = currentTime + 1;
                    completed.add(running);
                    running = null;
                }
            }

            currentTime++;

            // Guarda contra loop infinito
            if (running == null && readyQueue.isEmpty()
                    && blocked.isEmpty() && notArrived.isEmpty()
                    && completed.size() < processes.size()) {
                break;
            }
        }

        return Metrics.calculate(getName(), completed, currentTime);
    }
}