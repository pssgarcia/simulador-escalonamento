package src.escalonador;

import java.io.*;
import java.util.*;

/**
 * Lê o arquivo processos.txt e constrói a lista de processos.
 * 
 * Formato esperado por linha:
 *    PID;Cheagada;BurstTotal;Prioridade;[IO1,IO2,...]
 * 
 * Exemplos válidos:
 *   101;0;40;2;10,25     → P101, chega em 0, 40ms de CPU, prioridade 2, I/O aos 10ms e 25ms acumulados
 *   102;5;20;1           → P102, chega em 5, 20ms de CPU, prioridade 1, sem I/O
 *   103;2;15;1;          → P103, campo de I/O vazio (sem I/O), também válido
 *
 * Linhas em branco e comentários (iniciados com #) são ignorados.
 */

public class FileParser {
    
    public static List<Process> parse(String filename) throws IOException {
        List<Process> processes = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Ignorar linhas vazias e comentários
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                try {
                    processes.add(parseLine(line));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.err.printf("[AVISO] linha %d ignorada (formato inválido): %s%n", lineNumber, line);
                }
            }
        }

        if (processes.isEmpty()) {
            throw new IllegalStateException("Nenhum processo válido encontrado em: " + filename);
        }

        // Garante ordenação por tempo de chegada
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        return processes;
    }

    private static Process parseLine(String line) {
        String[] parts = line.split(";", -1); // -1 preserva campos vazios no final

        int pid      = Integer.parseInt(parts[0].trim());
        int arrival  = Integer.parseInt(parts[1].trim());
        int burst    = Integer.parseInt(parts[2].trim());
        int priority = Integer.parseInt(parts[3].trim());

        List<Integer> ioInstants = new ArrayList<>();
        if (parts.length > 4 && !parts[4].trim().isEmpty()) {
            for (String token : parts[4].trim().split(".")) {
                int instant = Integer.parseInt(token.trim());
                ioInstants.add(instant);
            }
            Collections.sort(ioInstants); // garante ordem crescente
        }
        
        return new Process(pid, arrival, burst, priority, ioInstants);
    }
}
