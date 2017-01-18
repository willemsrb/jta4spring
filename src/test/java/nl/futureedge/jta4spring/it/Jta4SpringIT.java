package nl.futureedge.jta4spring.it;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.support.TransactionTemplate;

public class Jta4SpringIT extends AbstractIT {


	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	public void happyFlow() {
		transactionTemplate.execute(status -> {
			//Create tabel
			jdbcTemplate.execute("create table test(id bigint)");

			// Send message
			jmsTemplate.send("QueueOne", (MessageCreator) session -> {
				return session.createTextMessage("test message");
			});
			return null;
		});
	}
}
