-- Orders the agent can look up
CREATE TABLE orders (
    order_id        TEXT PRIMARY KEY,          -- format: A-<number>, e.g. A-42
    customer_name   TEXT          NOT NULL,
    status          TEXT          NOT NULL,    -- PROCESSING, SHIPPED, DELIVERED, CANCELLED
    amount          NUMERIC(10,2) NOT NULL,    -- in INR
    refunded_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT now()
);

-- Every refund the agent issues gets its own row
CREATE TABLE refunds (
    refund_id   SERIAL PRIMARY KEY,
    order_id    TEXT          NOT NULL REFERENCES orders(order_id),
    amount      NUMERIC(10,2) NOT NULL,
    reason      TEXT,
    created_at  TIMESTAMP     NOT NULL DEFAULT now()
);

-- Test data: one order in each interesting state
INSERT INTO orders (order_id, customer_name, status, amount, refunded_amount) VALUES
  ('A-41', 'Asha',   'PROCESSING', 899.00,  0),
  ('A-42', 'Ravi',   'DELIVERED',  1499.00, 0),
  ('A-43', 'Meena',  'SHIPPED',    2599.00, 0),
  ('A-44', 'Kiran',  'CANCELLED',  749.00,  0),
  ('A-45', 'Suresh', 'DELIVERED',  12999.00, 12999.00);  -- already fully refunded