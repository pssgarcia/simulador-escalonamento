package src.escalonador.algoritmos;

import src.escalonador.Metrics;
import src.escalonador.Process;
import src.escalonador.Scheduler;

import java.util.*;

/**
 * Round-Robin com Quantum por Predição (Média Exponencial)
 *
 * Escalonamento circular onde o quantum NÃO é fixo: a cada troca de contexto,
 * o quantum é recalculado como o menor τ (tau) entre os processos na fila de prontos.
 *
 * Fórmula da Média Exponencial (atualizada após cada surto):
 *   τ_{n+1} = α * t_n + (1 - α) * τ_n
 *   α = 0.5,  τ_0 = 10ms (definido em Process.reset())
 *
 * Onde t_n é a duração real do surto que acabou de ser executado.
 *
 * Fluxo por tick:
 *   1. Processos bloqueados em I/O que atingiram ioReturnTime voltam à fila.
 *   2. Processos que chegaram no tempo atual entram na fila de prontos.
 *   3. Se o quantum esgotou, atualiza τ e devolve o processo ao fim da fila.
 *   4. Se a CPU está livre, seleciona o próximo e recalcula o quantum.
 *   5. O processo em execução consome 1 unidade de CPU.
 *   6. Se atingiu I/O ou concluiu, atualiza τ e libera a CPU.
 */
public class RoundRobin implements Scheduler {

    private static final double ALPHA = 0.5;

    @Override
    public String getName() {
        return "Round-Robin com Predição (Média Exponencial)";
    }

    @Override
    public Metrics run(List<Process> processes) {

        Queue<Process> readyQueue = new LinkedList<>();

        List<Process> completed  = new ArrayList<>();
        List<Process> blocked    = new ArrayList<>();
        List<Process> notArrived = new ArrayList<>(processes);
        notArrived.sort(Comparator.comparingInt(Process::getArrivalTime));

        Process running      = null;
        int currentTime      = 0;
        int quantumLeft      = 0;
        int currentBurstTime = 0; // t_n: duração real do surto atual em execução

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
            // 3. Troca de contexto por esgotamento do quantum
            //    Atualiza τ e devolve o processo ao fim da fila (Round-Robin)
            // ---------------------------------------------------------------
            if (running != null && quantumLeft == 0) {
                atualizarTau(running, currentBurstTime);
                readyQueue.add(running);
                running = null;
                currentBurstTime = 0;
            }

            // ---------------------------------------------------------------
            // 4. Se CPU livre, seleciona próximo e recalcula quantum
            //    Quantum = menor τ entre todos os processos na fila + running
            // ---------------------------------------------------------------
            if (running == null && !readyQueue.isEmpty()) {
                running = readyQueue.poll();
                quantumLeft = calcularQuantum(readyQueue, running);
                currentBurstTime = 0;
            }

            // ---------------------------------------------------------------
            // 5. Executa 1 unidade de CPU
            // ---------------------------------------------------------------
            if (running != null) {
                running.remainingBurst--;
                running.accumulatedCpu++;
                currentBurstTime++;
                quantumLeft--;

                // ---------------------------------------------------------------
                // 5a. Verifica se atingiu um instante de I/O
                //     Atualiza τ antes de bloquear (surto parcial também conta)
                // ---------------------------------------------------------------
                if (running.shouldDoIO()) {
                    atualizarTau(running, currentBurstTime);
                    running.triggerIO(currentTime + 1);
                    blocked.add(running);
                    running = null;
                    quantumLeft = 0;
                    currentBurstTime = 0;

                // ---------------------------------------------------------------
                // 5b. Verifica se terminou todo o burst
                // ---------------------------------------------------------------
                } else if (running.remainingBurst == 0) {
                    atualizarTau(running, currentBurstTime);
                    running.completionTime = currentTime + 1;
                    completed.add(running);
                    running = null;
                    quantumLeft = 0;
                    currentBurstTime = 0;
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

    /**
     * Calcula o quantum como o menor τ entre o processo selecionado
     * e todos os processos que estão na fila de prontos.
     * Mínimo de 1 para evitar quantum zero.
     */
    private int calcularQuantum(Queue<Process> readyQueue, Process running) {
        double minTau = running.tau;
        for (Process p : readyQueue) {
            if (p.tau < minTau) minTau = p.tau;
        }
        return (int) Math.max(1, Math.round(minTau));
    }

    /**
     * Atualiza τ do processo usando a Média Exponencial.
     * Chamado sempre que um surto termina (por quantum, I/O ou conclusão).
     *
     * @param process       processo cujo τ será atualizado
     * @param realBurstTime duração real do surto que acabou (t_n)
     */
    private void atualizarTau(Process process, int realBurstTime) {
        process.tau = ALPHA * realBurstTime + (1 - ALPHA) * process.tau;
    }
}