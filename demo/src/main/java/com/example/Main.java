package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Scanner;

/*
 * =====================================================================
 *  SISTEMA SUPERVISÓRIO INDUSTRIAL - versão console
 *  Algoritmos em Java para os REQUISITOS FUNCIONAIS auditados no AuditaReq
 *
 *  Parte 1 (Portal v1.0): RF-01 a RF-15
 *  Parte 2 (Portal v2.0): RF-16, RF-006, RF-007 e RF-008
 *    (os três últimos usam os IDs do relatório da parte 2, que repetem
 *     números da parte 1; por isso aparecem com "(Parte 2)" no nome)
 *
 *  Conteúdos usados: variáveis, Scanner, if/else, switch, operador ternário,
 *  for, while, do-while, vetores, matrizes, funções static, arquivos
 *  (FileWriter/BufferedWriter/BufferedReader), try/catch e split().
 *
 *  Adaptações (conteúdo que ainda não foi visto):
 *   - LocalDateTime.now(): pega a data e a hora do computador (RF-006 P2 e logs)
 *   - Vídeo da câmera é simulado com uma matriz de caracteres (RF-08)
 * =====================================================================
 */
public class Main {

    // Scanner único usado pelo programa inteiro
    static Scanner sc = new Scanner(System.in);

    // ---------------- Variáveis globais (estado do sistema) ----------------
    static String usuarioLogado = "";
    static String perfilLogado = "";

    static boolean emergenciaAtiva = false;          // RF-01
    static int acionamentosEmergencia = 0;           // RF-01
    static int statusRobo = 2;                       // RF-02 (2 = Parado)
    static String nomePrograma = "PICK_AND_PLACE_01";// RF-03
    static int estadoPrograma = 1;                   // RF-03
    static int linhaAtual = 0;                       // RF-03
    static int totalLinhas = 120;                    // RF-03
    static int pecasAprovadas = 0;                   // RF-05
    static int pecasRejeitadas = 0;                  // RF-05
    static int metaPecas = 100;                      // RF-05
    static double cicloPadrao = 12.0;                // RF-06 (segundos)
    static double mediaCiclo = 0;                    // RF-06 (usado no RF-12)
    static int cameraAtual = 0;                      // RF-08
    static boolean gravando = false;                 // RF-09
    static String arquivoLogs = "logs.txt";          // RF-11

    // RF-007 (Parte 2) - vetor de notificações
    static String[] notificacoes = new String[10];
    static boolean[] notificacaoLida = new boolean[10];
    static int qtdNotificacoes = 0;

    // RF-14 - matriz do painel de peças: {ID, Tipo, Status}
    static String[][] pecas = new String[50][3];
    static int qtdPecasPainel = 0;

    public static void main(String[] args) {
        boolean continuar = true;

        while (continuar) {
            exibirCabecalho();   // RF-16
            fazerLogin();        // RF-008 (Parte 2)
            registrarLog("INFO", "Login de " + usuarioLogado);
            menuSidebar();       // RF-15

            System.out.print("\nEntrar com outro usuário? (s/n): ");
            continuar = sc.nextLine().equalsIgnoreCase("s");
        }
        System.out.println("Sistema encerrado.");
        sc.close();
    }

    // =====================================================================
    // FUNÇÕES AUXILIARES DE LEITURA
    // Lê a linha inteira e converte com parseInt/parseDouble.
    // O try/catch impede que o programa feche se digitarem letras.
    // =====================================================================
    static int lerInteiro(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido! Digite um número inteiro.");
            }
        }
    }

    static double lerDouble(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            try {
                return Double.parseDouble(sc.nextLine().trim().replace(",", "."));
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido! Digite um número.");
            }
        }
    }

    static String lerTexto(String mensagem) {
        System.out.print(mensagem);
        return sc.nextLine();
    }

    static void pausar() {
        System.out.print("\nPressione ENTER para voltar ao menu...");
        sc.nextLine();
    }

    // =====================================================================
    // RF-01 - BOTÃO DE PARADA DE EMERGÊNCIA
    // Para o robô imediatamente, registra o evento, gera notificação e só
    // libera a operação depois de um rearme confirmado por um TÉCNICO.
    // =====================================================================
    static void rf01ParadaEmergencia() {
        int opcao;
        do {
            System.out.println("\n===== RF-01 PARADA DE EMERGÊNCIA =====");
            System.out.println("Estado: " + (emergenciaAtiva ? "!!! EMERGÊNCIA ATIVA !!!" : "NORMAL"));
            System.out.println("Acionamentos nesta sessão: " + acionamentosEmergencia);
            System.out.println("1 - ACIONAR PARADA DE EMERGÊNCIA");
            System.out.println("2 - Rearmar sistema");
            System.out.println("0 - Voltar");
            opcao = lerInteiro("Opção: ");

            switch (opcao) {
                case 1:
                    if (emergenciaAtiva) {
                        System.out.println("A emergência já está ativa!");
                    } else {
                        emergenciaAtiva = true;
                        statusRobo = 2;         // robô vai para PARADO
                        estadoPrograma = 3;     // programa vai para PAUSADO
                        acionamentosEmergencia++;
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        System.out.println("!!!  PARADA DE EMERGÊNCIA ACIONADA     !!!");
                        System.out.println("!!!  ROBÔ PARADO                       !!!");
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        registrarLog("EMERGENCIA", "Parada de emergência acionada por " + usuarioLogado);
                        adicionarNotificacao("EMERGÊNCIA acionada por " + usuarioLogado);
                    }
                    break;
                case 2:
                    if (!emergenciaAtiva) {
                        System.out.println("Não há emergência ativa.");
                    } else if (!perfilLogado.equals("TECNICO")) {
                        System.out.println("Somente um TÉCNICO pode rearmar o sistema.");
                    } else {
                        String resp = lerTexto("A área está segura? Confirmar rearme (s/n): ");
                        if (resp.equalsIgnoreCase("s")) {
                            emergenciaAtiva = false;
                            registrarLog("INFO", "Emergência rearmada por " + usuarioLogado);
                            adicionarNotificacao("Emergência rearmada");
                            System.out.println("Sistema rearmado. Robô liberado.");
                        } else {
                            System.out.println("Rearme cancelado.");
                        }
                    }
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    // =====================================================================
    // RF-02 - EXIBIÇÃO DO STATUS DO ROBÔ
    // 1-Em operação  2-Parado  3-Em manutenção  4-Falha
    // Com emergência ativa o robô não pode voltar a operar.
    // =====================================================================
    static String descricaoStatusRobo(int codigo) {
        switch (codigo) {
            case 1:  return "EM OPERAÇÃO";
            case 2:  return "PARADO";
            case 3:  return "EM MANUTENÇÃO";
            case 4:  return "FALHA";
            default: return "DESCONHECIDO";
        }
    }

    static void rf02StatusRobo() {
        int opcao;
        do {
            System.out.println("\n===== RF-02 STATUS DO ROBÔ =====");
            System.out.println("Status atual: " + descricaoStatusRobo(statusRobo)
                    + (emergenciaAtiva ? " (EMERGÊNCIA ATIVA)" : ""));
            System.out.println("Alterar: 1-Em operação 2-Parado 3-Manutenção 4-Falha | 0-Voltar");
            opcao = lerInteiro("Opção: ");

            if (opcao >= 1 && opcao <= 4) {
                if (opcao == 1 && emergenciaAtiva) {
                    System.out.println("Bloqueado: rearme a emergência (RF-01) antes de operar.");
                } else {
                    statusRobo = opcao;
                    registrarLog(opcao == 4 ? "ERRO" : "INFO", "Robô alterado para " + descricaoStatusRobo(opcao));
                    if (opcao == 4) {
                        adicionarNotificacao("Robô em FALHA");
                    }
                }
            } else if (opcao != 0) {
                System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    // =====================================================================
    // RF-03 - STATUS DO PROGRAMA
    // Mostra o programa carregado, o estado e o progresso (%) com barra.
    // =====================================================================
    static String nomeEstadoPrograma(int e) {
        switch (e) {
            case 1:  return "PARADO";
            case 2:  return "EXECUTANDO";
            case 3:  return "PAUSADO";
            case 4:  return "FINALIZADO";
            case 5:  return "ERRO";
            default: return "DESCONHECIDO";
        }
    }

    static double calcularProgresso(int atual, int total) {
        if (total <= 0) {
            return 0;
        }
        return (double) atual / total * 100;
    }

    // Barra com 20 posições: cada '#' vale 5%
    static String barraProgresso(double porcentagem) {
        int cheios = (int) (porcentagem / 5);
        String barra = "[";
        for (int i = 0; i < 20; i++) {
            barra += (i < cheios) ? "#" : "-";
        }
        return barra + "]";
    }

    static void rf03StatusPrograma() {
        int opcao;
        do {
            double p = calcularProgresso(linhaAtual, totalLinhas);
            System.out.println("\n===== RF-03 STATUS DO PROGRAMA =====");
            System.out.println("Programa : " + nomePrograma);
            System.out.println("Estado   : " + nomeEstadoPrograma(estadoPrograma));
            System.out.printf("Progresso: %s %.1f%% (linha %d de %d)%n", barraProgresso(p), p, linhaAtual, totalLinhas);
            System.out.println("1 - Carregar outro programa");
            System.out.println("2 - Atualizar linha em execução");
            System.out.println("3 - Alterar estado");
            System.out.println("0 - Voltar");
            opcao = lerInteiro("Opção: ");

            switch (opcao) {
                case 1:
                    nomePrograma = lerTexto("Nome do programa: ").toUpperCase();
                    totalLinhas = lerInteiro("Total de linhas: ");
                    linhaAtual = 0;
                    estadoPrograma = 1;
                    registrarLog("INFO", "Programa " + nomePrograma + " carregado");
                    break;
                case 2:
                    int linha = lerInteiro("Linha atual: ");
                    if (linha < 0 || linha > totalLinhas) {
                        System.out.println("Linha fora do intervalo 0 a " + totalLinhas);
                    } else {
                        linhaAtual = linha;
                        if (linhaAtual == totalLinhas) {
                            estadoPrograma = 4; // chegou ao fim
                        }
                    }
                    break;
                case 3:
                    System.out.println("1-Parado 2-Executando 3-Pausado 4-Finalizado 5-Erro");
                    int e = lerInteiro("Novo estado: ");
                    if (e == 2 && emergenciaAtiva) {
                        System.out.println("Bloqueado: emergência ativa.");
                    } else if (e >= 1 && e <= 5) {
                        estadoPrograma = e;
                        registrarLog(e == 5 ? "ERRO" : "INFO", "Programa " + nomePrograma + " -> " + nomeEstadoPrograma(e));
                    } else {
                        System.out.println("Estado inválido.");
                    }
                    break;
            }
        } while (opcao != 0);
    }

    // =====================================================================
    // RF-04 - STATUS DA COMUNICAÇÃO
    // Classifica cada equipamento pelo tempo de resposta (ping):
    //   0 a 200 ms -> ONLINE | 201 a 1000 ms -> INSTÁVEL | >1000 ou -1 -> OFFLINE
    // =====================================================================
    static String classificarComunicacao(int pingMs) {
        if (pingMs < 0 || pingMs > 1000) {
            return "OFFLINE";
        } else if (pingMs > 200) {
            return "INSTÁVEL";
        } else {
            return "ONLINE";
        }
    }

    static void rf04StatusComunicacao() {
        String[] dispositivos = {"Robô", "CLP", "Câmera", "Servidor"};
        int[] pings = new int[dispositivos.length];

        System.out.println("\n===== RF-04 STATUS DA COMUNICAÇÃO =====");
        System.out.println("Informe o tempo de resposta em ms (-1 se não respondeu):");
        for (int i = 0; i < dispositivos.length; i++) {
            pings[i] = lerInteiro(dispositivos[i] + ": ");
        }

        int online = 0;
        System.out.println("\nDISPOSITIVO | PING     | ESTADO");
        for (int i = 0; i < dispositivos.length; i++) {
            String estado = classificarComunicacao(pings[i]);
            String ping = (pings[i] < 0) ? "---" : pings[i] + " ms";
            System.out.printf("%-11s | %-8s | %s%n", dispositivos[i], ping, estado);
            if (estado.equals("ONLINE")) {
                online++;
            } else if (estado.equals("OFFLINE")) {
                registrarLog("ERRO", "Perda de comunicação com " + dispositivos[i]);
                adicionarNotificacao(dispositivos[i] + " OFFLINE");
            }
        }
        System.out.println("Online: " + online + " de " + dispositivos.length);
        pausar();
    }

    // =====================================================================
    // RF-05 - CONTADOR DE PEÇAS PROCESSADAS
    // Conta peças aprovadas e rejeitadas e compara com a meta do turno.
    // =====================================================================
    static int totalPecas() {
        return pecasAprovadas + pecasRejeitadas;
    }

    static void rf05ContadorPecas() {
        int opcao;
        do {
            System.out.println("\n===== RF-05 CONTADOR DE PEÇAS =====");
            System.out.println("Aprovadas : " + pecasAprovadas);
            System.out.println("Rejeitadas: " + pecasRejeitadas);
            System.out.println("Total     : " + totalPecas() + " / meta " + metaPecas
                    + (totalPecas() >= metaPecas ? "  -> META ATINGIDA!" : ""));
            System.out.println("1 - Peça aprovada");
            System.out.println("2 - Peça rejeitada");
            System.out.println("3 - Registrar lote");
            System.out.println("4 - Zerar contador");
            System.out.println("0 - Voltar");
            opcao = lerInteiro("Opção: ");

            if (opcao >= 1 && opcao <= 3 && emergenciaAtiva) {
                System.out.println("Emergência ativa: produção parada.");
                continue;
            }
            switch (opcao) {
                case 1:
                    pecasAprovadas++;
                    break;
                case 2:
                    pecasRejeitadas++;
                    break;
                case 3:
                    int ok = lerInteiro("Aprovadas no lote: ");
                    int nok = lerInteiro("Rejeitadas no lote: ");
                    if (ok < 0 || nok < 0) {
                        System.out.println("Quantidades não podem ser negativas.");
                    } else {
                        pecasAprovadas += ok;
                        pecasRejeitadas += nok;
                    }
                    break;
                case 4:
                    registrarLog("INFO", "Contador zerado (total era " + totalPecas() + ")");
                    pecasAprovadas = 0;
                    pecasRejeitadas = 0;
                    break;
            }
        } while (opcao != 0);
    }

    // =====================================================================
    // RF-06 - MEDIÇÃO DO TEMPO DE CICLO
    // Guarda os tempos de vários ciclos em um vetor e calcula último,
    // média, menor, maior e ciclos por hora, comparando com o padrão.
    // =====================================================================
    static double mediaVetor(double[] v) {
        double soma = 0;
        for (int i = 0; i < v.length; i++) {
            soma += v[i];
        }
        return soma / v.length;
    }

    static double menorVetor(double[] v) {
        double m = v[0];
        for (int i = 1; i < v.length; i++) {
            if (v[i] < m) {
                m = v[i];
            }
        }
        return m;
    }

    static double maiorVetor(double[] v) {
        double m = v[0];
        for (int i = 1; i < v.length; i++) {
            if (v[i] > m) {
                m = v[i];
            }
        }
        return m;
    }

    static void rf06TempoCiclo() {
        System.out.println("\n===== RF-06 TEMPO DE CICLO =====");
        System.out.println("Tempo de ciclo padrão: " + cicloPadrao + " s");
        int n = lerInteiro("Quantos ciclos deseja registrar? ");
        if (n <= 0) {
            System.out.println("Quantidade inválida.");
            return;
        }

        double[] ciclos = new double[n];
        int acimaDoPadrao = 0;
        for (int i = 0; i < n; i++) {
            ciclos[i] = lerDouble("Tempo do ciclo " + (i + 1) + " (s): ");
            while (ciclos[i] <= 0) {
                ciclos[i] = lerDouble("O tempo deve ser maior que zero: ");
            }
            if (ciclos[i] > cicloPadrao) {
                acimaDoPadrao++;
            }
        }

        mediaCiclo = mediaVetor(ciclos);
        System.out.printf("Último ciclo: %.2f s%n", ciclos[n - 1]);
        System.out.printf("Média       : %.2f s%n", mediaCiclo);
        System.out.printf("Menor       : %.2f s%n", menorVetor(ciclos));
        System.out.printf("Maior       : %.2f s%n", maiorVetor(ciclos));
        System.out.printf("Ciclos/hora : %.0f%n", 3600 / mediaCiclo);
        System.out.println("Ciclos acima do padrão: " + acimaDoPadrao + " de " + n);

        if (mediaCiclo > cicloPadrao) {
            System.out.println("ATENÇÃO: média acima do tempo padrão!");
            registrarLog("ALERTA", "Tempo de ciclo médio acima do padrão: " + mediaCiclo + " s");
        } else {
            System.out.println("Dentro do tempo padrão.");
        }
        pausar();
    }

    // =====================================================================
    // RF-07 - INDICADOR DE TAXA DE ACERTO
    // Taxa = acertos / total * 100
    //   >= 95% EXCELENTE | >= 85% ATENÇÃO | < 85% CRÍTICO
    // =====================================================================
    static double calcularTaxaAcerto(int acertos, int total) {
        if (total == 0) {
            return 0; // evita divisão por zero
        }
        return (double) acertos / total * 100;
    }

    static String classificarTaxa(double taxa) {
        if (taxa >= 95) {
            return "EXCELENTE";
        } else if (taxa >= 85) {
            return "ATENÇÃO";
        } else {
            return "CRÍTICO";
        }
    }

    static void rf07TaxaAcerto() {
        System.out.println("\n===== RF-07 TAXA DE ACERTO =====");
        System.out.println("1 - Usar dados do contador (RF-05)");
        System.out.println("2 - Digitar valores");
        int op = lerInteiro("Opção: ");

        int acertos, total;
        if (op == 1) {
            acertos = pecasAprovadas;
            total = totalPecas();
        } else {
            total = lerInteiro("Total de peças inspecionadas: ");
            acertos = lerInteiro("Peças corretas: ");
        }

        if (total <= 0 || acertos < 0 || acertos > total) {
            System.out.println("Dados insuficientes ou inválidos para calcular a taxa.");
        } else {
            double taxa = calcularTaxaAcerto(acertos, total);
            System.out.printf("Taxa de acerto: %.1f%% - %s%n", taxa, classificarTaxa(taxa));
            if (taxa < 85) {
                adicionarNotificacao("Taxa de acerto crítica");
            }
        }
        pausar();
    }

    // =====================================================================
    // RF-08 - VISUALIZAÇÃO DE CÂMERA/VÍDEO
    // Adaptação: a imagem é uma matriz de caracteres (como pixels).
    // A peça '#' é desenhada na posição escolhida pelo usuário.
    // =====================================================================
    static char[][] gerarQuadroCamera(int lin, int col) {
        int linhas = 8, colunas = 20;
        char[][] quadro = new char[linhas][colunas];
        for (int i = 0; i < linhas; i++) {
            for (int j = 0; j < colunas; j++) {
                if (i == 0 || i == linhas - 1) {
                    quadro[i][j] = '-';
                } else if (j == 0 || j == colunas - 1) {
                    quadro[i][j] = '|';
                } else {
                    quadro[i][j] = '.';
                }
            }
        }
        // peça de 2 linhas x 3 colunas, só desenha o que couber dentro da borda
        for (int i = lin; i < lin + 2; i++) {
            for (int j = col; j < col + 3; j++) {
                if (i > 0 && i < linhas - 1 && j > 0 && j < colunas - 1) {
                    quadro[i][j] = '#';
                }
            }
        }
        return quadro;
    }

    static void rf08VisualizacaoCamera() {
        String[] cameras = {"Câmera 1 - Esteira", "Câmera 2 - Garra do robô"};
        int lin = 3, col = 8;
        int opcao;
        do {
            System.out.println("\n===== RF-08 VISUALIZAÇÃO DA CÂMERA =====");
            System.out.println(cameras[cameraAtual] + (gravando ? "   ● REC" : "")); // RF-09 aparece aqui
            char[][] quadro = gerarQuadroCamera(lin, col);
            for (int i = 0; i < quadro.length; i++) {
                for (int j = 0; j < quadro[i].length; j++) {
                    System.out.print(quadro[i][j]);
                }
                System.out.println();
            }
            System.out.println("1 - Trocar câmera  2 - Mover peça  0 - Voltar");
            opcao = lerInteiro("Opção: ");
            if (opcao == 1) {
                cameraAtual = (cameraAtual == 0) ? 1 : 0;
            } else if (opcao == 2) {
                lin = lerInteiro("Linha da peça (1 a 5): ");
                col = lerInteiro("Coluna da peça (1 a 16): ");
            }
        } while (opcao != 0);
    }

    // =====================================================================
    // RF-09 - INDICADOR DE GRAVAÇÃO DE VÍDEO
    // Mostra "● REC" enquanto grava. Ao parar, mostra a duração e o
    // tamanho estimado (duração * 0.5 MB por segundo).
    // =====================================================================
    static void rf09GravacaoVideo() {
        double taxaMBporSegundo = 0.5;
        int opcao;
        do {
            System.out.println("\n===== RF-09 GRAVAÇÃO DE VÍDEO =====");
            System.out.println(gravando ? "● REC - gravando" : "○ Gravação parada");
            System.out.println("1 - " + (gravando ? "Parar gravação" : "Iniciar gravação"));
            System.out.println("0 - Voltar");
            opcao = lerInteiro("Opção: ");

            if (opcao == 1) {
                if (!gravando) {
                    gravando = true;
                    registrarLog("INFO", "Gravação iniciada");
                } else {
                    // Adaptação: o usuário informa a duração em vez de cronometrar
                    int duracao = lerInteiro("Duração da gravação (s): ");
                    if (duracao < 0) {
                        duracao = 0;
                    }
                    gravando = false;
                    System.out.println("Duração: " + formatarDuracao(duracao));
                    System.out.printf("Tamanho estimado: %.1f MB%n", duracao * taxaMBporSegundo);
                    registrarLog("INFO", "Gravação encerrada com " + duracao + " s");
                }
            }
        } while (opcao != 0);
    }

    // =====================================================================
    // RF-10 - STATUS DE PROCESSAMENTO DA CÂMERA
    // Para cada imagem mostra as etapas (Captura -> Pré-processamento ->
    // Detecção -> Classificação) e o resultado: OK, defeito ou erro.
    // =====================================================================
    static void rf10ProcessamentoCamera() {
        String[] etapas = {"Captura", "Pré-processamento", "Detecção", "Classificação"};

        System.out.println("\n===== RF-10 PROCESSAMENTO DA CÂMERA =====");
        int n = lerInteiro("Quantas imagens processar? ");
        if (n <= 0) {
            System.out.println("Quantidade inválida.");
            return;
        }

        int ok = 0, defeito = 0, erro = 0;
        for (int i = 0; i < n; i++) {
            System.out.println("\nImagem " + (i + 1) + " de " + n + ":");
            for (int e = 0; e < etapas.length; e++) {
                System.out.println("  [" + (e + 1) + "/" + etapas.length + "] " + etapas[e]
                        + "... " + ((e + 1) * 100 / etapas.length) + "%");
            }
            int resultado = lerInteiro("Resultado (1-OK 2-Defeito 3-Erro): ");
            switch (resultado) {
                case 1:
                    ok++;
                    System.out.println("Status: CONCLUÍDO - peça OK");
                    break;
                case 2:
                    defeito++;
                    System.out.println("Status: CONCLUÍDO - peça com defeito");
                    break;
                default:
                    erro++;
                    System.out.println("Status: ERRO no processamento");
                    registrarLog("ERRO", "Falha ao processar imagem " + (i + 1));
            }
        }
        System.out.println("\nResumo -> OK: " + ok + " | Defeito: " + defeito + " | Erro: " + erro);
        System.out.printf("Processadas com sucesso: %.1f%%%n", (double) (ok + defeito) / n * 100);
        pausar();
    }

    // =====================================================================
    // RF-11 - REGISTRO DE LOGS EM TEMPO REAL
    // Cada evento é mostrado na hora e gravado em logs.txt no formato
    //   dataHora;NIVEL;mensagem
    // (FileWriter com append = true, como na aula 07)
    // =====================================================================
    static void registrarLog(String nivel, String mensagem) {
        String linha = dataHoraAtual() + ";" + nivel + ";" + mensagem.replace(";", ",");
        System.out.println("[LOG] " + linha);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(arquivoLogs, true))) {
            bw.write(linha);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Erro ao gravar log: " + e.getMessage());
        }
    }

    static void mostrarLogs() {
        try (BufferedReader br = new BufferedReader(new FileReader(arquivoLogs))) {
            String linha;
            System.out.println("DATA/HORA           | NÍVEL      | MENSAGEM");
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split(";");
                if (partes.length >= 3) {
                    System.out.printf("%-19s | %-10s | %s%n", partes[0], partes[1], partes[2]);
                }
            }
        } catch (IOException e) {
            System.out.println("Nenhum log registrado ainda.");
        }
    }

    static void rf11RegistroLogs() {
        int opcao;
        do {
            System.out.println("\n===== RF-11 LOGS EM TEMPO REAL =====");
            System.out.println("1 - Ver logs");
            System.out.println("2 - Registrar ocorrência manual");
            System.out.println("0 - Voltar");
            opcao = lerInteiro("Opção: ");

            if (opcao == 1) {
                mostrarLogs();
            } else if (opcao == 2) {
                int n = lerInteiro("Nível (1-INFO 2-ALERTA 3-ERRO): ");
                String nivel = (n == 2) ? "ALERTA" : (n == 3) ? "ERRO" : "INFO";
                String msg = lerTexto("Descrição: ");
                registrarLog(nivel, msg + " (por " + usuarioLogado + ")");
            }
        } while (opcao != 0);
    }

    // =====================================================================
    // RF-12 - MONITORAMENTO DA EFICIÊNCIA (OEE)
    // OEE = Disponibilidade x Performance x Qualidade
    //   Disponibilidade = tempo produzindo / tempo planejado
    //   Performance     = (ciclo ideal x total de peças) / tempo produzindo
    //   Qualidade       = peças boas / total de peças
    //   >= 85% classe mundial | >= 60% aceitável | < 60% baixo
    // =====================================================================
    static double calcularOEE(double disp, double perf, double qual) {
        return disp * perf * qual * 100;
    }

    static void rf12EficienciaOEE() {
        System.out.println("\n===== RF-12 EFICIÊNCIA (OEE) =====");
        double planejado = lerDouble("Tempo planejado de produção (min): ");
        double parado = lerDouble("Tempo parado (min): ");
        double cicloIdeal = lerDouble("Tempo de ciclo ideal (s) [padrão " + cicloPadrao + "]: ");

        int total, boas;
        String usar = lerTexto("Usar peças do contador RF-05? (s/n): ");
        if (usar.equalsIgnoreCase("s")) {
            total = totalPecas();
            boas = pecasAprovadas;
        } else {
            total = lerInteiro("Total de peças produzidas: ");
            boas = lerInteiro("Peças boas: ");
        }

        double produzindo = planejado - parado;
        if (planejado <= 0 || produzindo <= 0 || total <= 0 || boas < 0 || boas > total || cicloIdeal <= 0) {
            System.out.println("Dados inválidos para o cálculo do OEE.");
            pausar();
            return;
        }

        double disponibilidade = produzindo / planejado;
        double performance = (cicloIdeal * total) / (produzindo * 60); // min -> s
        if (performance > 1) {
            performance = 1; // limite de 100%
        }
        double qualidade = (double) boas / total;
        double oee = calcularOEE(disponibilidade, performance, qualidade);

        System.out.printf("Disponibilidade: %.1f%%%n", disponibilidade * 100);
        System.out.printf("Performance    : %.1f%%%n", performance * 100);
        System.out.printf("Qualidade      : %.1f%%%n", qualidade * 100);
        System.out.printf("OEE            : %.1f%% - ", oee);
        if (oee >= 85) {
            System.out.println("CLASSE MUNDIAL");
        } else if (oee >= 60) {
            System.out.println("ACEITÁVEL");
        } else {
            System.out.println("BAIXO");
            adicionarNotificacao("OEE baixo");
        }
        pausar();
    }

    // =====================================================================
    // RF-13 - MÉTRICAS DE DISPONIBILIDADE MECÂNICA
    //   MTTR = soma dos tempos de reparo / nº de falhas
    //   MTBF = (tempo total - tempo em reparo) / nº de falhas
    //   Disponibilidade = MTBF / (MTBF + MTTR) * 100
    // =====================================================================
    static void rf13DisponibilidadeMecanica() {
        System.out.println("\n===== RF-13 DISPONIBILIDADE MECÂNICA =====");
        double tempoTotal = lerDouble("Tempo total do período (h): ");
        int falhas = lerInteiro("Quantidade de falhas no período: ");

        if (tempoTotal <= 0 || falhas < 0) {
            System.out.println("Dados inválidos.");
            pausar();
            return;
        }
        if (falhas == 0) {
            System.out.println("Nenhuma falha: disponibilidade de 100%.");
            pausar();
            return;
        }

        double[] reparos = new double[falhas];
        double somaReparos = 0;
        for (int i = 0; i < falhas; i++) {
            reparos[i] = lerDouble("Tempo de reparo da falha " + (i + 1) + " (h): ");
            somaReparos += reparos[i];
        }
        if (somaReparos >= tempoTotal) {
            System.out.println("Tempo de reparo não pode ser maior que o período.");
            pausar();
            return;
        }

        double mttr = somaReparos / falhas;
        double mtbf = (tempoTotal - somaReparos) / falhas;
        double disponibilidade = mtbf / (mtbf + mttr) * 100;

        System.out.printf("MTBF (tempo médio entre falhas): %.2f h%n", mtbf);
        System.out.printf("MTTR (tempo médio de reparo)   : %.2f h%n", mttr);
        System.out.printf("Disponibilidade mecânica       : %.2f%%%n", disponibilidade);
        pausar();
    }

    // =====================================================================
    // RF-14 - PAINEL DE MONITORAMENTO DE PEÇAS
    // Matriz de Strings onde cada linha é uma peça: {ID, Tipo, Status}
    // Mostra em formato de tabela e conta quantas há em cada status.
    // =====================================================================
    static void rf14PainelPecas() {
        int opcao;
        do {
            System.out.println("\n===== RF-14 PAINEL DE PEÇAS =====");
            System.out.println("1 - Cadastrar peça");
            System.out.println("2 - Mostrar painel");
            System.out.println("3 - Alterar status de uma peça");
            System.out.println("0 - Voltar");
            opcao = lerInteiro("Opção: ");

            switch (opcao) {
                case 1:
                    if (qtdPecasPainel == pecas.length) {
                        System.out.println("Painel cheio!");
                        break;
                    }
                    pecas[qtdPecasPainel][0] = "P" + (qtdPecasPainel + 1);
                    pecas[qtdPecasPainel][1] = lerTexto("Tipo da peça: ");
                    pecas[qtdPecasPainel][2] = "AGUARDANDO";
                    System.out.println("Peça " + pecas[qtdPecasPainel][0] + " cadastrada.");
                    qtdPecasPainel++;
                    break;
                case 2:
                    int aguardando = 0, aprovadas = 0, rejeitadas = 0;
                    System.out.printf("%-5s | %-15s | %s%n", "ID", "TIPO", "STATUS");
                    for (int i = 0; i < qtdPecasPainel; i++) {
                        System.out.printf("%-5s | %-15s | %s%n", pecas[i][0], pecas[i][1], pecas[i][2]);
                        if (pecas[i][2].equals("APROVADA")) {
                            aprovadas++;
                        } else if (pecas[i][2].equals("REJEITADA")) {
                            rejeitadas++;
                        } else {
                            aguardando++;
                        }
                    }
                    System.out.println("Aguardando: " + aguardando + " | Aprovadas: " + aprovadas
                            + " | Rejeitadas: " + rejeitadas);
                    break;
                case 3:
                    String id = lerTexto("ID da peça (ex: P1): ");
                    int pos = -1;
                    for (int i = 0; i < qtdPecasPainel; i++) {
                        if (pecas[i][0].equalsIgnoreCase(id)) {
                            pos = i;
                        }
                    }
                    if (pos == -1) {
                        System.out.println("Peça não encontrada.");
                    } else {
                        int s = lerInteiro("Novo status (1-Aguardando 2-Aprovada 3-Rejeitada): ");
                        pecas[pos][2] = (s == 2) ? "APROVADA" : (s == 3) ? "REJEITADA" : "AGUARDANDO";
                    }
                    break;
            }
        } while (opcao != 0);
    }

    // =====================================================================
    // RF-15 - NAVEGAÇÃO POR SIDEBAR
    // Adaptação para console: a barra lateral vira um menu numerado
    // (vetor de opções + do-while + switch). Mostra no topo usuário,
    // hora, emergência e notificações não lidas.
    // =====================================================================
    static void menuSidebar() {
        String[] opcoes = {
            "Logout / Perfil (RF-008 P2)",
            "Parada de Emergência (RF-01)",
            "Status do Robô (RF-02)",
            "Status do Programa (RF-03)",
            "Status da Comunicação (RF-04)",
            "Contador de Peças (RF-05)",
            "Tempo de Ciclo (RF-06)",
            "Taxa de Acerto (RF-07)",
            "Câmera / Vídeo (RF-08)",
            "Gravação de Vídeo (RF-09)",
            "Processamento da Câmera (RF-10)",
            "Logs em Tempo Real (RF-11)",
            "Eficiência OEE (RF-12)",
            "Disponibilidade Mecânica (RF-13)",
            "Painel de Peças (RF-14)",
            "Relógio do Sistema (RF-006 P2)",
            "Central de Notificações (RF-007 P2)"
        };

        boolean sair = false;
        while (!sair) {
            System.out.println("\n+-------------------------------------------+");
            System.out.println("| " + horaAtual() + " | " + usuarioLogado + " (" + perfilLogado + ")");
            if (emergenciaAtiva) {
                System.out.println("| !!! EMERGÊNCIA ATIVA - MÁQUINA PARADA !!!");
            }
            int naoLidas = contarNaoLidas();
            if (naoLidas > 0) {
                System.out.println("| (!) " + naoLidas + " notificação(ões) não lida(s)");
            }
            System.out.println("+-------------------------------------------+");
            for (int i = 1; i < opcoes.length; i++) {
                System.out.printf("| %2d - %s%n", i, opcoes[i]);
            }
            System.out.printf("| %2d - %s%n", 0, opcoes[0]);
            System.out.println("+-------------------------------------------+");

            int opcao = lerInteiro("Escolha: ");
            switch (opcao) {
                case 0:  sair = rf008PerfilLogout(); break;
                case 1:  rf01ParadaEmergencia(); break;
                case 2:  rf02StatusRobo(); break;
                case 3:  rf03StatusPrograma(); break;
                case 4:  rf04StatusComunicacao(); break;
                case 5:  rf05ContadorPecas(); break;
                case 6:  rf06TempoCiclo(); break;
                case 7:  rf07TaxaAcerto(); break;
                case 8:  rf08VisualizacaoCamera(); break;
                case 9:  rf09GravacaoVideo(); break;
                case 10: rf10ProcessamentoCamera(); break;
                case 11: rf11RegistroLogs(); break;
                case 12: rf12EficienciaOEE(); break;
                case 13: rf13DisponibilidadeMecanica(); break;
                case 14: rf14PainelPecas(); break;
                case 15: rf006RelogioSistema(); break;
                case 16: rf007CentralNotificacoes(); break;
                default: System.out.println("Opção inexistente!");
            }
        }
    }

    // =====================================================================
    // RF-16 (Parte 2) - IDENTIFICAÇÃO E LOGOTIPO DA INSTITUIÇÃO
    // Logotipo desenhado com caracteres, guardado em um vetor de Strings.
    // =====================================================================
    static void exibirCabecalho() {
        String[] logotipo = {
            "  ____  _____ _   _    _    ___ ",
            " / ___|| ____| \\ | |  / \\  |_ _|",
            " \\___ \\|  _| |  \\| | / _ \\  | | ",
            "  ___) | |___| |\\  |/ ___ \\ | | ",
            " |____/|_____|_| \\_/_/   \\_\\___|"
        };
        System.out.println();
        for (int i = 0; i < logotipo.length; i++) {
            System.out.println(logotipo[i]);
        }
        System.out.println("==============================================");
        System.out.println(" SENAI - Sistema Supervisório Industrial v2.0");
        System.out.println("==============================================");
    }

    // =====================================================================
    // RF-006 (Parte 2) - RELÓGIO DO SISTEMA
    // Mostra data/hora atual, o turno e converte segundos em hh:mm:ss.
    // Adaptação: LocalDateTime.now() pega a hora do computador.
    // =====================================================================
    static String doisDigitos(int valor) {
        return (valor < 10) ? "0" + valor : "" + valor;
    }

    static String horaAtual() {
        LocalDateTime agora = LocalDateTime.now();
        return doisDigitos(agora.getHour()) + ":" + doisDigitos(agora.getMinute()) + ":"
                + doisDigitos(agora.getSecond());
    }

    static String dataHoraAtual() {
        LocalDateTime agora = LocalDateTime.now();
        return doisDigitos(agora.getDayOfMonth()) + "/" + doisDigitos(agora.getMonthValue()) + "/"
                + agora.getYear() + " " + horaAtual();
    }

    static String formatarDuracao(int totalSegundos) {
        int horas = totalSegundos / 3600;
        int minutos = (totalSegundos % 3600) / 60;
        int segundos = totalSegundos % 60;
        return doisDigitos(horas) + ":" + doisDigitos(minutos) + ":" + doisDigitos(segundos);
    }

    static String turno(int hora) {
        if (hora >= 5 && hora < 12) {
            return "Manhã";
        } else if (hora >= 12 && hora < 18) {
            return "Tarde";
        } else if (hora >= 18 && hora < 22) {
            return "Noite";
        } else {
            return "Madrugada";
        }
    }

    static void rf006RelogioSistema() {
        System.out.println("\n===== RF-006 (P2) RELÓGIO DO SISTEMA =====");
        System.out.println("Data/hora: " + dataHoraAtual());
        System.out.println("Turno    : " + turno(LocalDateTime.now().getHour()));
        int segundos = lerInteiro("Há quantos segundos a máquina está ligada? ");
        if (segundos >= 0) {
            System.out.println("Tempo em operação: " + formatarDuracao(segundos));
        } else {
            System.out.println("Valor inválido.");
        }
        pausar();
    }

    // =====================================================================
    // RF-007 (Parte 2) - CENTRAL DE NOTIFICAÇÕES
    // Vetor de mensagens + vetor boolean de lidas. Quando enche (10),
    // a mais antiga é removida deslocando o vetor.
    // =====================================================================
    static void adicionarNotificacao(String mensagem) {
        if (qtdNotificacoes == notificacoes.length) {
            for (int i = 1; i < notificacoes.length; i++) {
                notificacoes[i - 1] = notificacoes[i];
                notificacaoLida[i - 1] = notificacaoLida[i];
            }
            qtdNotificacoes--;
        }
        notificacoes[qtdNotificacoes] = horaAtual() + " - " + mensagem;
        notificacaoLida[qtdNotificacoes] = false;
        qtdNotificacoes++;
    }

    static int contarNaoLidas() {
        int cont = 0;
        for (int i = 0; i < qtdNotificacoes; i++) {
            if (!notificacaoLida[i]) {
                cont++;
            }
        }
        return cont;
    }

    static void rf007CentralNotificacoes() {
        int opcao;
        do {
            System.out.println("\n===== RF-007 (P2) CENTRAL DE NOTIFICAÇÕES =====");
            System.out.println("Não lidas: " + contarNaoLidas() + " de " + qtdNotificacoes);
            if (qtdNotificacoes == 0) {
                System.out.println("Nenhuma notificação.");
            }
            for (int i = 0; i < qtdNotificacoes; i++) {
                System.out.println((i + 1) + ") " + (notificacaoLida[i] ? "[lida]     " : "[NÃO LIDA] ") + notificacoes[i]);
            }
            System.out.println("1 - Marcar uma como lida");
            System.out.println("2 - Marcar todas como lidas");
            System.out.println("3 - Limpar notificações");
            System.out.println("0 - Voltar");
            opcao = lerInteiro("Opção: ");

            switch (opcao) {
                case 1:
                    int num = lerInteiro("Número da notificação: ");
                    if (num >= 1 && num <= qtdNotificacoes) {
                        notificacaoLida[num - 1] = true;
                    } else {
                        System.out.println("Número inválido.");
                    }
                    break;
                case 2:
                    for (int i = 0; i < qtdNotificacoes; i++) {
                        notificacaoLida[i] = true;
                    }
                    break;
                case 3:
                    qtdNotificacoes = 0;
                    break;
            }
        } while (opcao != 0);
    }

    // =====================================================================
    // RF-008 (Parte 2) - PERFIL DO USUÁRIO E LOGOUT
    // Login simples (nome + perfil), tela de perfil e logout com
    // confirmação. Retorna true se o logout foi confirmado.
    // =====================================================================
    static void fazerLogin() {
        usuarioLogado = lerTexto("Nome do usuário: ").trim();
        while (usuarioLogado.isEmpty()) {
            usuarioLogado = lerTexto("O nome não pode ficar vazio: ").trim();
        }
        int p = lerInteiro("Perfil (1-Operador 2-Técnico): ");
        perfilLogado = (p == 2) ? "TECNICO" : "OPERADOR";
        System.out.println("Bem-vindo, " + usuarioLogado + "!");
    }

    static boolean rf008PerfilLogout() {
        System.out.println("\n===== RF-008 (P2) PERFIL DO USUÁRIO =====");
        System.out.println("Usuário: " + usuarioLogado);
        System.out.println("Perfil : " + perfilLogado);
        System.out.println("Agora  : " + dataHoraAtual());

        String resp = lerTexto("Confirmar logout? (s/n): ");
        if (resp.equalsIgnoreCase("s")) {
            registrarLog("INFO", "Logout de " + usuarioLogado);
            usuarioLogado = "";
            perfilLogado = "";
            System.out.println("Logout realizado.");
            return true;
        }
        System.out.println("Logout cancelado.");
        return false;
    }
}
