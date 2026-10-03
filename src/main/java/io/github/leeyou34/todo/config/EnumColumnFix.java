package io.github.leeyou34.todo.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * H2 파일 DB의 enum 칸을 문자열 칸으로 바꿉니다.
 *
 * Hibernate는 H2에서 @Enumerated(STRING) 칸을 ENUM('A','B') 형식(또는 값 목록 CHECK 제약)으로 만듭니다.
 * ddl-auto=update는 이미 있는 칸을 고치지 않으므로, 나중에 enum 값을 추가하면(예: 영업장 구분 LIRICOS)
 * 운영 DB에는 그 값을 저장할 수 없어 500 오류가 납니다. 시작할 때 한 번 칸을 VARCHAR로 바꾸고
 * 값 목록 CHECK 제약을 지워 두면, 이후 enum 값을 더해도 그대로 저장됩니다. 여러 번 실행해도 안전합니다.
 */
@Component
public class EnumColumnFix implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(EnumColumnFix.class);

	private final DataSource dataSource;

	public EnumColumnFix(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			int changed = fix();
			if (changed > 0) {
				log.info("enum 칸 정리: {}곳을 문자열 칸으로 바꿨습니다.", changed);
			}
		} catch (SQLException e) {
			log.warn("enum 칸 정리를 건너뜁니다: {}", e.getMessage());
		}
	}

	/** 바꾼 칸·제약 수를 돌려줍니다. H2가 아니면 아무것도 하지 않습니다. */
	public int fix() throws SQLException {
		try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
			String db = c.getMetaData().getDatabaseProductName();
			if (db == null || !db.toLowerCase(Locale.ROOT).contains("h2")) {
				return 0;
			}
			int changed = 0;
			List<String[]> enumColumns = new ArrayList<>();
			try (ResultSet rs = st.executeQuery("SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
				+ "WHERE TABLE_SCHEMA = 'PUBLIC' AND DATA_TYPE = 'ENUM'")) {
				while (rs.next()) {
					enumColumns.add(new String[] { rs.getString(1), rs.getString(2) });
				}
			}
			for (String[] col : enumColumns) {
				st.execute("ALTER TABLE " + quote(col[0]) + " ALTER COLUMN " + quote(col[1])
					+ " SET DATA TYPE VARCHAR(40)");
				changed++;
			}
			List<String[]> checks = new ArrayList<>();
			try (ResultSet rs = st.executeQuery("SELECT tc.TABLE_NAME, tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE "
				+ "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc "
				+ "ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME "
				+ "WHERE tc.TABLE_SCHEMA = 'PUBLIC' AND tc.CONSTRAINT_TYPE = 'CHECK'")) {
				while (rs.next()) {
					String clause = rs.getString(3);
					if (clause != null && clause.toUpperCase(Locale.ROOT).replace(" ", "").contains("IN('")) {
						checks.add(new String[] { rs.getString(1), rs.getString(2) });
					}
				}
			}
			for (String[] ck : checks) {
				st.execute("ALTER TABLE " + quote(ck[0]) + " DROP CONSTRAINT " + quote(ck[1]));
				changed++;
			}
			return changed;
		}
	}

	private static String quote(String name) {
		return "\"" + name.replace("\"", "\"\"") + "\"";
	}
}
