package src.escalonador;

import java.util.List;

/**
 * Encapsula as métricas de desempenho de um algoritmo de escalonamento.
 * 
 * Métricas calculadas:
 *  - Tempo de Espera Médio (average wait time)
 *  - Tempo de Retorno Médio / Turnaround (average turnaorund)
 *  - Vazão / Throughput (processos concluídos por unidade de tempo)
 */

public class Metrics {
    
    private final String algorithmName;
    private final double avgWaitTime;
    private final double avgTurnaround;
    private final double throughput;

    private Metrics(String algorithmName, double avgWaitTime, double avgTurnaround, double throughput) {
        this.algorithmName = algorithmName;
        this.avgWaitTime   = avgWaitTime;
        this.avgTurnaround = avgTurnaround;
        this.throughput    = throughput;
    }

    /**
     * Calcula as métricas a partir da lista de processos já concluídos.
     * 
     * @param algorithName nome do algoritmo (para exibição)
     * @param completed    lista com todos os processos após a simulação
     * @param totalTime    instante de conclusão do último processo (tempo total da simulação)
     * @return objeto Metrics preenchido
     */
    public static Metrics calculate(String algorithmName, List<Process> completed, int totalTime) {
        if (completed.isEmpty()) {
            return new Metrics(algorithmName, 0, 0, 0);
        }

        double avgWait = completed.stream()
            .mapToInt(Process::getWaitTime)
            .average()
            .orElse(0.0);

        double avgTurnaround = completed.stream()
            .mapToInt(Process::getTurnaround)
            .average()
            .orElse(0.0);

        double throughput = (totalTime > 0) ? (double) completed.size() / totalTime : 0.0;

        return new Metrics(algorithmName, avgWait, avgTurnaround, throughput);
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

    public String getAlgorithmName() { return algorithmName; }
    public double getAvgWaitTime()   { return avgWaitTime; }
    public double getAvgTurnaround() { return avgTurnaround; }
    public double getThroughput()    { return throughput; }

    @Override
    public String toString() {
        return String.format(
            "%-38s | Espera Média: %7.2f | Turnaorund Médio: %7.2f | Vazão: %.5f proc/ut",
            algorithmName, avgWaitTime, avgTurnaround, throughput
        );
    }
}
