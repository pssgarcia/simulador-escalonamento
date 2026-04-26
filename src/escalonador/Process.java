package src.escalonador;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um processo no simulador de escalonamento.
 * 
 * Separa claramente os dados estáticos (vindos do arquivo) do estado
 * dinâmico (modificado durante a simulação), facilitando o reset entre
 * algoritmos diferentes.
 */
public class Process {

    // ---------------------------------------------------------------
    // Dados estáticos — definidos na leitura do arquivo
    // ---------------------------------------------------------------   

    public final int pid; // Id do processo
    public final int arrivalTime; // Tempo de chegada
    public final int totalBurst; // Total de CPU
    public final int priority; // Prioridade
    public final List<Integer> ioInstants; // Instantes do CPU acumulados em que ocorre I/O

    // ---------------------------------------------------------------
    // Estado dinâmico — alterado durante a simulação
    // ---------------------------------------------------------------
    
    public int remainingBurst; // CPU restante para terminar
    public int accumulatedCpu; // CPU acumulada já consumida nesta simulação
    public int nextIoIndex;    // Índice do próximo instante de I/O a ocorrer
    public int ioReturnTime;   // Tempo do sistema em que retorna do I/O (-1 = não está em I/O)
    public double tau;         // t: previsão do próximo surto (Média Exponencial)

    // ---------------------------------------------------------------
    // Resultados — preenchidos ao final da execução do processo
    // ---------------------------------------------------------------
    
    public int completionTime; // Instantes em que o processo terminou

    // ---------------------------------------------------------------
    // Construtor e métodos de ciclo de vida
    // ---------------------------------------------------------------

    public Process(int pid, int arrivalTime, int totalBurst, int priority, List<Integer> ioInstants) {
        this.pid         = pid;
        this.arrivalTime = arrivalTime;
        this.totalBurst  = totalBurst;
        this.priority    = priority;
        this.ioInstants  = new ArrayList<>(ioInstants);
        reset();
    }

    /**
     * Reinicia todo o estado dinâmico
     * Chamado no construtor e também pelo Main anstes de cada algoritmo.''
     */
    public void reset() {
        this.remainingBurst = totalBurst;
        this.accumulatedCpu = 0;
        this.nextIoIndex    = 0;
        this.ioReturnTime   = -1;
        this.tau            = 10.0;
        this.completionTime = -1;
    }

    /**
     * Cria uma cópia independente deste processo (estado resetado).
     * usada pelo Main para garantir que cada algoritmo receba processos "novos".
     */
    public Process copy() {
        return new Process(pid, arrivalTime, totalBurst, priority, ioInstants);
    }

    /**
     * Dispara o próximo I/O: avança o índice e calcula o tempo de retorno.
     * 
     * @param currentTime tempo atual do sistema
     * @return tempo em que o preocesso voltará a fila de prontos
     */
    public int triggerIO(int currentTime) {
        nextIoIndex++;
        ioReturnTime = currentTime + 5; // cada I/O dura 5 unidades de tempo
        return ioReturnTime;
    }

    /** Retorna true se o processo está atualmente bloqueado em I/O. */
    public boolean isBlocked() {
        return ioReturnTime != -1;
    }

    /** Libera o processo do estado de I/O (chamado quando ioReturnTime <= currentTime). */
    public void finishIO() {
        ioReturnTime = -1;
    }
    
    // ---------------------------------------------------------------
    // Métricas individuais
    // ---------------------------------------------------------------
 
    /**
     * Turnaround = tempo desde a chegada até a conclusão
     */
    public int getTurnaround() {
        if (completionTime == -1) {
            throw new IllegalStateException("Processo" + pid + " ainda não terminou.");
        }
        return completionTime - arrivalTime;
    }

    /**
     * Tempo de espera = Turnaround - tempo de CPU - tempo total de I/O.
     */
    public int getWaitTime() {
        int totalIOTime = ioInstants.size() * 5;
        return getTurnaround() - totalBurst - totalIOTime;
    }

    
    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

    public int getPid() { return pid; }
    public int getArrivalTime() { return arrivalTime; }
    public int getTotalBurst() { return totalBurst; }
    public int getPriority() { return priority; }
    public List<Integer> getIoInstants() { return ioInstants; }

    @Override
    public String toString() {
        return String.format("PID: %3d | Chegada: %3d | CPU: %3d | Prior: %2d | I/O: %s",
            pid, arrivalTime, totalBurst, priority, ioInstants.toString()
        );
    }
}
