import java.util.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import db.conect;


public class SistemaReciclagem {

    // ─── Fatores de impacto por kg: [CO2 (kg), Água (L), Energia (kWh)] ───────
    private static final Map<String, double[]> IMPACTO = new LinkedHashMap<>();

    // ─── Valores de mercado por kg em Reais (R$) ──────────────────────────────
    private static final Map<String, Double> VALOR_MERCADO = new LinkedHashMap<>();

    public boolean rodando;
    
    // ─── Carrega os dados do banco de dados ─────────────────────────────────────
    private static void MateriaisBanco(){
        try (Connection conn = conect.conectar();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT material, co2_kg, agua_l, energia_kwh, valor_kg FROM materiais")) {

            while (rs.next()) {
                String material = rs.getString("material");
                double co2 = rs.getDouble("co2_kg");
                double agua = rs.getDouble("agua_l");
                double energia = rs.getDouble("energia_kwh");
                double valor = rs.getDouble("valor_kg");

                IMPACTO.put(material, new double[]{co2, agua, energia});
                VALOR_MERCADO.put(material, valor);
            }
        } catch (SQLException e) {
            System.out.println("Erro ao carregar materiais do banco: " + e.getMessage());
        }
    }


    // ─── Estado do sistema ────────────────────────────────────────────────────
    private static final Map<String, Double>totais = new LinkedHashMap<>();
    private static final List<String[]>     historico = new ArrayList<>();
    private static final DecimalFormat      df = new DecimalFormat("#,##0.00");
    private static final Scanner            scanner = new Scanner(System.in);

    // ─── Formatação do terminal ───────────────────────────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String GREEN  = "\u001B[32m";
    private static final String CYAN   = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String DIM    = "\u001B[2m";

    public static void main(String[] args ){
        //carrega os materiais do banco de dados
        MateriaisBanco();
        


        // Inicializa todos os totais com zero
        IMPACTO.keySet().forEach(m -> totais.put(m, 0.0));
        exibirBanner();

        boolean rodando = true;
        while (rodando){
            exibirMenu();
            int opcao = lerOpcao();

            switch(opcao){
                case 1 -> registrarMaterial();
                case 2 -> exibirTotais();
                case 3 -> exibirImpactoAmbiental();
                case 4 -> exibirHistorico();
                case 5 -> {
                    System.out.println("\n" + GREEN + "Ate logo! Continue reciclando."+ RESET + "\n");
                    rodando = false;
                }
                default -> System.out.println(YELLOW + "Opcao invalida. Escolha entre 1 e 5." + RESET);
            }
        }
        scanner.close();
    }

    //  FUNCIONALIDADES

    private static void registrarMaterial(){
        System.out.println("\n"+ BOLD+ CYAN+"------REGISTRAR MATERIAL-----"+ RESET);

        //lista os materiais disponiveis
        String[] materiais = IMPACTO.keySet().toArray(new String[0]);
        for(int i = 0; i < materiais.length; i++){
            System.out.printf("  %s[%d]%s %-10s %s(R$ %s/kg)%s%n",
                    GREEN, i + 1, RESET, materiais[i],DIM, df.format(VALOR_MERCADO.get(materiais[i])), RESET);
        }

        //escolha do material
        System.out.print("\n Material (1- "+ materiais.length+"): ");
        int escolha = lerOpcao();
        if (escolha < 1 || escolha > materiais.length){
            System.out.println(YELLOW+ " NUMERO INVALIDO."+RESET);
            return;
        }
        String material = materiais[escolha - 1];

        //quantidade
        System.out.print("Quantidade em Kg:");
        double quantidade = lerDouble();
        if (quantidade <= 0){
            System.out.println(YELLOW+"Quantidade deve ser maior que zero." + RESET);
            return;
        }
        totais.merge(material, quantidade, Double::sum);

        double valorEstimado = quantidade * VALOR_MERCADO.get(material);
        String hora = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        historico.add(new String[]{hora, material, df.format(quantidade), df.format(valorEstimado)});
        System.out.println("\n "+ GREEN + " OK " + df.format(quantidade)+ "kg de "+ material + " registrados com sucesso: "+ RESET);
        System.out.println(" "+ DIM + " Valor estimado: R$ "+ df.format(valorEstimado)+ RESET);
        pausar();
    }
    private static void exibirTotais(){
        System.out.println("\n" + BOLD + CYAN + "---Total Reciclado e Valor Estimado---"+ RESET);

        double somaGeralKg = 0;
        double somaGeralReais = 0;

        System.out.printf(" %-10s %-12s %-12s %-12s%n ", "Material", "Qtd(kg)", "Valor(R$)", "Progresso");
        linha();

        for(Map.Entry<String, Double> e : totais.entrySet()){
            String material = e.getKey();
            double qtd = e.getValue();
            double valor = qtd * VALOR_MERCADO.get(material);

            somaGeralKg += qtd;
            somaGeralReais += valor;

            String barra = gerarBarra(qtd, maiorTotal());

            System.out.printf(" %-10s %-12s R$ %-9s %s%s%s%n ", material, df.format(qtd), df.format(valor), GREEN, barra, RESET);
        }

        linha();
        System.out.printf(" %-10s %-12s R$ %-9s %s%n", BOLD + GREEN, df.format(somaGeralKg), df.format(somaGeralReais), RESET);
        pausar();

    }

    private static void exibirImpactoAmbiental(){
        System.out.println("\n"+ BOLD + CYAN + " ---Impacto Ambiental Estimado---"+ RESET);
        System.out.println(DIM+ "(baseado na quantidade total registrada)\n"+ RESET);

        double co2 = 0, agua = 0, energia = 0;
        for (Map.Entry<String, Double> e : totais.entrySet()){
            double[] f = IMPACTO.get(e.getKey());
            co2 += e.getValue() * f[0];
            agua += e.getValue() * f[1];
            energia += e.getValue() * f[2];
        }
        System.out.printf(" %s CO2 evitado   :%s  %s%s kg%s%n", GREEN, RESET, BOLD, df.format(co2), RESET);
        System.out.printf("  %s Água poupada  :%s %s%s litros%s%n", CYAN, RESET, BOLD, df.format(agua), RESET);
        System.out.printf("  %s Energia       :%s %s%s kWh%s%n", YELLOW, RESET, BOLD, df.format(energia), RESET);

        //equivalencia didatica
        System.out.println();
        if(co2 > 0)
            System.out.printf(" "+ DIM + "≈ %.0f km não dirigidos de carro%s%n", co2/0.21, RESET);
        if(agua > 100)
            System.out.printf("  " + DIM + "≈ %.0f banhos de 5 min economizados%s%n", agua/ 60.0, RESET);

        pausar();
    }

    private static void exibirHistorico(){
        System.out.println("\n" + BOLD + CYAN + "---Histórico de Registros---" + RESET);

        if(historico.isEmpty()){
            System.out.println(DIM + " Nenhum registro ainda.\n"+ RESET);
            pausar();
            return;
        }

        //cabeçalho da tabela
        System.out.printf(" %s%-20s %-10s %-10s %s%s%n", DIM, " Data/Hora", "Material", "Qtd(kg)", "Valor(R$)", RESET);
        linha();

        //mais recente primeiro
        for (int i = historico.size() - 1; i >= 0; i--){
            String[] r = historico.get(i);
            System.out.printf("%-20s %-10s %-10s R$ %s%n",  r[0], r[1], r[2], r[3]);
        }

        linha();
        System.out.println(DIM + " " + historico.size()+ " registro(s) no total."+ RESET);
        pausar();
    }
    //  HELPERS DE ENTRADA

    private static int lerOpcao(){
        try{
            String linha = scanner.nextLine().trim();
            return Integer.parseInt(linha);
        } catch (NumberFormatException e){
            return -1;
        }
    }
    private static double lerDouble(){
        try{
            String linha = scanner.nextLine().trim().replace(',', '.');
            return  Double.parseDouble(linha);
        }catch(NumberFormatException e){
            return -1;
        }
    }

    //  HELPERS DE EXIBIÇÃO

    private static void exibirBanner(){
        System.out.println();
        System.out.println(GREEN + BOLD);
        System.out.println("Companhia de Reciclagem");
        System.out.println(RESET);
    }
    private static void exibirMenu(){
        System.out.println("\n" + BOLD + "  Menu Principal" + RESET);
        System.out.println(DIM + "---------------------------------" + RESET);
        System.out.println("  [1] Registrar material reciclado");
        System.out.println("  [2] Ver total reciclado e valores");
        System.out.println("  [3] Ver impacto ambiental");
        System.out.println("  [4] Ver Histórico");
        System.out.println("  [5] Sair");
        System.out.println(DIM + "---------------------------------" + RESET);
        System.out.print("  Escolha: ");
    }

    private static String gerarBarra(double valor, double max){
        if (max == 0) return "░░░░░░░░░░";
        int cheios = (int) Math.round ((valor/max) * 10);
        return "█".repeat(cheios) + "░".repeat(10 - cheios);
    }

    private static double maiorTotal() {
        return totais.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    private static void linha() {
        System.out.println(DIM + "  ─────────────────────────────────────────────────────────" + RESET);
    }

    private static void pausar() {
        System.out.print("\n" + DIM + "  Pressione Enter para continuar..." + RESET);
        scanner.nextLine();
    }
}















