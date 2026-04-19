-------------------------------------------------------
-- Servidor:                     127.0.0.1
-- Versão do servidor:           8.0.45 - MySQL Community Server - GPL
-- OS do Servidor:               Linux
-- HeidiSQL Versão:              12.7.0.6850
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- Copiando estrutura para tabela reciclagem_db.historico
CREATE TABLE IF NOT EXISTS `historico` (
  `ID_His` int NOT NULL AUTO_INCREMENT,
  `ID_Mat` int NOT NULL,
  `Qnt_Kg` double NOT NULL DEFAULT '0',
  `Valor_Kg` double NOT NULL DEFAULT (0),
  `co2_kg` double NOT NULL,
  `agua_l` double NOT NULL,
  `energia_kwh` double NOT NULL,
  `Data_Cria` datetime NOT NULL DEFAULT (now()),
  PRIMARY KEY (`ID_His`) USING BTREE,
  KEY `FK_Hístorico_materiais` (`ID_Mat`) USING BTREE,
  CONSTRAINT `FK_Hístorico_materiais` FOREIGN KEY (`ID_Mat`) REFERENCES `materiais` (`ID_Mat`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela reciclagem_db.materiais
CREATE TABLE IF NOT EXISTS `materiais` (
  `ID_Mat` int NOT NULL AUTO_INCREMENT,
  `material` varchar(50) NOT NULL,
  `co2_kg` double NOT NULL,
  `agua_l` double NOT NULL,
  `energia_kwh` double NOT NULL,
  `valor_kg` double NOT NULL,
  PRIMARY KEY (`ID_Mat`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Exportação de dados foi desmarcado.

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;