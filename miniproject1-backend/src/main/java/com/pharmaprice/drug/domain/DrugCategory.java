package com.pharmaprice.drug.domain;

import java.util.Set;

// DATABASE.md §3.4 drug.category 허용값. DB에는 체크 제약이 없어 여기서 검증한다.
public final class DrugCategory {

	public static final Set<String> ALLOWED = Set.of("해열진통", "소화제", "감기약", "연고", "소독약", "비타민", "기타");

	private DrugCategory() {
	}
}
