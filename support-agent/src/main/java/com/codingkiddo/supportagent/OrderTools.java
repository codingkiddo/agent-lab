package com.codingkiddo.supportagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderTools {

	private static final Logger log = LoggerFactory.getLogger(OrderTools.class);

	private final JdbcClient jdbc;

	public OrderTools(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Tool(description = "Look up an order by its ID and return its status and amounts.")
	public String getOrder(
			@ToolParam(description = "The order ID. Format: A-<number>, for example A-42") String orderId) {

		log.info(">>> TOOL CALLED: getOrder(\"{}\")", orderId);

		return jdbc.sql("""
				SELECT order_id, customer_name, status, amount, refunded_amount
				FROM orders WHERE order_id = ?
				""").param(orderId).query().listOfRows().stream().findFirst().map(Map::toString)
				.orElse("No order found with ID " + orderId);
	}

	@Tool(description = "Issue a refund for an order. Amount is in Indian Rupees.")
	public String issueRefund(
			@ToolParam(description = "The order ID. Format: A-<number>, for example A-42") String orderId,
			@ToolParam(description = "Refund amount in Indian Rupees") java.math.BigDecimal amount,
			@ToolParam(description = "Short reason for the refund") String reason) {

		log.info(">>> TOOL CALLED: issueRefund(\"{}\", {}, \"{}\")", orderId, amount, reason);

		// ⚠️ Deliberately NO checks: no status check, no amount check, no duplicate
		// check.
		return jdbc.sql("""
				WITH upd AS (
				    UPDATE orders
				    SET refunded_amount = refunded_amount + :amount
				    WHERE order_id = :orderId
				    RETURNING order_id
				)
				INSERT INTO refunds (order_id, amount, reason)
				SELECT order_id, :amount, :reason FROM upd
				RETURNING refund_id
				""").param("orderId", orderId).param("amount", amount).param("reason", reason).query(Integer.class)
				.optional().map(id -> "Refund issued. Refund ID: " + id + ", amount: ₹" + amount)
				.orElse("No order found with ID " + orderId);
	}
}