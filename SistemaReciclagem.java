import db.conect;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;


public class SistemaReciclagem {

    // ─── Fatores de impacto por kg: [CO2 (kg), Água (L), Energia (kWh)] ───────
    private static final Map<String, double[]> IMPACTO = new LinkedHashMap<>();

    // ─── Valores de mercado por kg em Reais (R$) ──────────────────────────────
    private static final Map<String, Double> VALOR_MERCADO = new LinkedHashMap<>();

    // ─── Estado do sistema ────────────────────────────────────────────────────
    private static final Map<String, Double> totaisMateriais = new LinkedHashMap<>();
    private static final Map<String,Integer> IDMATERIAL = new LinkedHashMap<>();
private static final Map<String, Double> totaisImpacto = new LinkedHashMap<>();
    private static final List<String[]>     historico = new ArrayList<>();
    private static final DecimalFormat      df = new DecimalFormat("#,##0.00");
    private static final Scanner            scanner = new Scanner(System.in);
    
    // ─── Carrega os Matérias do banco de dados ─────────────────────────────────────
    private static void MateriaisBanco(){
        IMPACTO.clear();
        VALOR_MERCADO.clear();
        try (Connection conn = conect.conectar();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT ID_Mat, material, co2_kg, agua_l, energia_kwh, valor_kg FROM materiais")) {

            while (rs.next()) {
                Integer id_mat = rs.getInt("ID_Mat");
                String material = rs.getString("material");
                double co2 = rs.getDouble("co2_kg");
                double agua = rs.getDouble("agua_l");
                double energia = rs.getDouble("energia_kwh");
                double valor = rs.getDouble("valor_kg");

                IMPACTO.put(material, new double[]{co2, agua, energia});
                VALOR_MERCADO.put(material, valor);
                IDMATERIAL.put(material,id_mat);
            }
        } catch (SQLException e) {
            System.out.println("Erro ao carregar materiais do banco: " + e.getMessage());
        }
    }

    // ───Historico de registros ───────────────────────────────────────────────────
    private static void HistoricoBanco(){
        historico.clear();
        try (Connection conn = conect.conectar();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("""
                                             SELECT \r
                                                 h.data_hora AS DATA,\r
                                                 m.material,\r
                                                 h.Qnt_Kg AS quantidade_kg,\r
                                                 h.Qnt_Kg * h.Valor_Kg AS valor_estimado\r
                                             FROM historico h\r
                                             JOIN materiais m \r
                                                 ON h.ID_Mat = m.ID_Mat\r
                                             ORDER BY h.data_hora DESC;""" //

        )) {

            while (rs.next()) {
                String dataHora = rs.getString("DATA");
                String material = rs.getString("material");
                String quantidade = df.format(rs.getDouble("quantidade_kg"));
                String valorEstimado = df.format(rs.getDouble("valor_estimado"));

                historico.add(new String[]{dataHora, material, quantidade, valorEstimado});
            }
        } catch (SQLException e) {
            System.out.println("Erro ao carregar histórico do banco: " + e.getMessage());
        }
    }

     // ───Salvar registro no histórico do banco de dados ───────────────────────────────────
    private static void SalvarHistoricoBanco(int material, double quantidade){
        String sql = """
                     INSERT INTO historico (\r
                         ID_Mat,\r
                         Qnt_Kg,\r
                         Valor_Kg,\r
                         co2_kg,\r
                         agua_l,\r
                         energia_kwh\r
                     )\r
                     SELECT \r
                         m.ID_Mat,\r
                         ?, -- quantidade que voc\u00ea quer registrar\r
                         m.valor_kg,\r
                         m.co2_kg,\r
                         m.agua_l,\r
                         m.energia_kwh\r
                     FROM materiais m\r
                     WHERE m.ID_Mat = ?;""" //
        ;
        try (Connection conn = conect.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, quantidade);
            pstmt.setInt(2, material);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erro ao salvar histórico no banco: " + e.getMessage());
        }
    }

    //───consultar o impacto ambiental total do banco de dados ───────────────────────────────────
    private static void ImpactoBanco(){
        totaisImpacto.clear();
        String sql = """
                     SELECT \r
                         SUM(Qnt_Kg * co2_kg) AS total_co2,\r
                         SUM(Qnt_Kg * agua_l) AS total_agua,\r
                         SUM(Qnt_Kg * energia_kwh) AS total_energia\r
                     FROM historico""" //
        ;
        try (Connection conn = conect.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                double co2 = rs.getDouble("total_co2");
                double agua = rs.getDouble("total_agua");
                double energia = rs.getDouble("total_energia");

                totaisImpacto.put("total_co2", co2);
                totaisImpacto.put("total_agua", agua);
                totaisImpacto.put("total_energia", energia);
            }
        } catch (SQLException e) {
            System.out.println("Erro ao consultar impacto ambiental do banco: " + e.getMessage());
        }
    }

    private static void exibirTotaisBanco(){
        totaisMateriais.clear();
        String sql = """
                     SELECT m.material, SUM(h.Qnt_Kg) AS total_kg\r
                     FROM historico h\r
                     JOIN materiais m ON h.ID_Mat = m.ID_Mat\r
                     GROUP BY m.material""" //
        ;
        try (Connection conn = conect.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String material = rs.getString("material");
                double totalKg = rs.getDouble("total_kg");
                totaisMateriais.put(material, totalKg);
            }
        } catch (SQLException e) {
            System.out.println("Erro ao consultar totais do banco: " + e.getMessage());
        }
    }

    // ─── Formatação do terminal ───────────────────────────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String GREEN  = "\u001B[32m";
    private static final String CYAN   = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String DIM    = "\u001B[2m";

    public static void main(String[] args ){


        // Inicializa todos os totais com zero

        exibirBanner();

        boolean rodando = true;
        while (rodando){

            MateriaisBanco(); //atualiza os materiais e seus fatores de impacto/valor do banco de dados
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

        boolean rep = true, rep2 =true;
        String[] materiais = IDMATERIAL.keySet().toArray(new String[0]);
        double quantidade;
        int escolha;

        //faz um loop para escolha correta do material
        do{
        System.out.println("\n"+ BOLD+ CYAN+"------REGISTRAR MATERIAL-----"+ RESET);
        for(int i = 0; i < materiais.length; i++){
            System.out.printf("  %s[%d]%s %-10s %s(R$ %s/kg)%s%n",
                    GREEN, i + 1, RESET, materiais[i],DIM, df.format(VALOR_MERCADO.get(materiais[i])), RESET);
        }

        //escolha do material
        System.out.print("\n Material (1- "+ materiais.length+"): ");
        escolha = lerOpcao();
        if (escolha < 1 || escolha > materiais.length){
            System.out.println(YELLOW+ " NUMERO INVALIDO."+RESET);
            continue;
        }
        rep = false;
    }while(rep);

    // faz um loop para leitura correta de quantidade
        do{
        System.out.print("Quantidade em Kg:");
         quantidade = lerDouble();
        if (quantidade <= 0){
            System.out.println(YELLOW+"Quantidade deve ser maior que zero." + RESET);
            continue;
        }
        rep2 = false;
    }while(rep2);

        SalvarHistoricoBanco(IDMATERIAL.get(materiais[escolha - 1]), quantidade);
        
    }

    private static void exibirTotais(){
        exibirTotaisBanco(); //atualiza os totais de cada material com os dados do banco de dados
        System.out.println("\n" + BOLD + CYAN + "---Total Reciclado e Valor Estimado---"+ RESET);

        double somaGeralKg = 0;
        double somaGeralReais = 0;

        System.out.printf(" %-10s %-12s %-12s %-12s%n ", "Material", "Qtd(kg)", "Valor(R$)", "Progresso");
        linha();

        for(Map.Entry<String, Double> e :   totaisMateriais.entrySet()){
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
        ImpactoBanco(); //atualiza os totais de impacto ambiental com os dados do banco de dados

        double co2 = totaisImpacto.getOrDefault("total_co2", 0.0);
        double agua = totaisImpacto.getOrDefault("total_agua", 0.0);
        double energia = totaisImpacto.getOrDefault("total_energia", 0.0);
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
        HistoricoBanco(); //carrega o histórico do banco de dados para a lista local
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
        return totaisMateriais.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    private static void linha() {
        System.out.println(DIM + "  ─────────────────────────────────────────────────────────" + RESET);
    }

    private static void pausar() {
        System.out.print("\n" + DIM + "  Pressione Enter para continuar..." + RESET);
        scanner.nextLine();
    }

    private static void limparConsole() {
        try {
            new ProcessBuilder("cmd", "/c", "cls")
                .inheritIO()
                .start()
                .waitFor();
        } catch (Exception e) {
            System.out.println("Não foi possível limpar o console.");
        }
    }
}















