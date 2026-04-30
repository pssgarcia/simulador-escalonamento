package src.escalonador;

import java.util.List;

/**
 * Interface comum a todos os algoritmos de escalonamento.
 * 
 * Cada algoritmo recebe uma lista de processos já copiados (prontos para simulação)
 * e retorna as métricas calculadas ao final.
 * 
 * Contrato importante:
 *    - A lista recebida em run() pertence exclusivamente ao algoritmo.
 *      Ele pode modificar os objetos Process livremente.
 *    - Ao final de run(), todo processo da lista deve ter completionTime definido.
 */

public interface Scheduler {
    /**
     * Executa a simulação de escalonamento.
     * 
     * @param processes lista de processos (cópias independetes, já resetadas)
     * @return métricas calculadas a partir dessa simulação
    */
    Metrics run(List<Process> processes);

    /** Nome legível do algoritmo (usado na saída do relatório). */
    String getName();
}