package src.escalonador;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ponto de entrada do simulador.
 * 
 * Fluxo: 
 *   1. Lê o arquivo processos.txt (ou o caminho passado como argumento)
 *   2. Para cada algoritmo: cria uma cópia fresca dos processos e executa
 *   3. Imprime a tabela comparativa dos resultados
 * 
 * Uso:
 *   java escalonador.Main                     → usa processos.txt no diretório atual
 *   java escalonador.Main caminho/arquivo.txt → usa o arquivo indicado
 */
public class Main {
    
    public static void main(String[] args) {
        String filename = (args.length > 0) ? args[0] : "processos.txt";

        // --- Leitura do arquivo ---
        List<Process> original;
        try {
            original = FileParser.parse(filename);
        } catch (IOException e) {
            System.err.println("[ERRO] Não foi possível ler o arquivo: " + filename);
            System.err.println("        " + e.getMessage());
            return;
        }

        System.out.println("=".repeat(95));
        System.out.println(" SIMULADOR DE ESCALONAMENTO DE CPU");
        System.out.printf(" Arquivo: %-20s | Processos carregados: %d%n", filename, original.size());
        System.out.println("=".repeat(95));
        System.out.println(" Processos:");

        original.forEach(p -> System.out.println("     " + p));
        System.out.println("=".repeat(95));

        // --- Instanciar os algoritmos ---
        List<Scheduler> schedulers = List.of(
            new FCFS(),
            new SRTF(),
            new RoundRobin(),
            new MLQ()
        );

        // --- Executar cada algoritmo com uma cópia independente dos processos --- 
        System.out.println("\n RESULTADOS:\n");
        System.out.printf("  %-38s | %-20s | %-22s | %s%n",
            "Algoritmo", "Espera Média (ms)", "Turnaround Médio (ms)", "Vazão (proc/ut)");
        System.out.println("  " + "-".repeat(110));

        for (Scheduler scheduler : schedulers) {
            // Cópia fresca: cada algoritmo parte do zero
            List<Process> processes = original.stream()
                .map(Process::copy)
                .collect(Collectors.toList());

            try {
                Metrics metrics = scheduler.run(processes);
                System.out.printf("  %-38s | %20.2f | %22.2f | %.5f%n",
                    metrics.getAlgorithmName(),
                    metrics.getAvgWaitTime(),
                    metrics.getAvgTurnaround(), 
                    metrics.getThroughput());
            } catch (UnsupportedOperationException e) {
                System.out.printf("  %-38s | %s%n", scheduler.getName(), "[NÃO IMPLEMENTADO AINDA]");
            } catch (Exception e) {
                System.out.printf("  %-38s | [ERRO: %s]%n", scheduler.getName(), e.getMessage());
            }
        }

        System.out.println("  " + "-".repeat(110));
        System.out.println();
    }
}
