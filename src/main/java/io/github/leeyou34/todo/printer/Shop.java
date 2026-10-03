package io.github.leeyou34.todo.printer;

import java.time.LocalDate;

import io.github.leeyou34.todo.printer.Enums.ShopType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** 영업장(특약점·직영 영업소) */
@Entity
@Table(name = "pr_shop")
public class Shop extends OwnedEntity {

	@Column(nullable = false, length = 20)
	public String code;

	@Column(nullable = false, length = 50)
	public String name;

	@Column(length = 20)
	public String division;

	@Column(length = 30)
	public String team;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	public ShopType type = ShopType.SPECIALTY;

	@Column(length = 20)
	public String managerName;

	@Column(length = 20)
	public String phone;

	@Column(length = 120)
	public String address;

	public LocalDate openedOn;

	@Column(nullable = false)
	public boolean active = true;

	/** 폐쇄일(폐점·통합 등). 폐쇄된 영업장은 새 신청을 받지 않습니다. */
	public LocalDate closedOn;

	@Column(length = 200)
	public String note;
}
