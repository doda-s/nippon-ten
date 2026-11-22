#!/usr/bin/env python3
"""Teste ponta a ponta do pagamento online contra o sandbox do Mercado Pago.

Roda contra a API local (quarkus:dev) configurada com as credenciais de TESTE e com o webhook
do painel apontando para ela (ngrok). Usa só rotas públicas, como um visitante:

  1. Pix: pedido ONLINE de um visitante "APRO", que o sandbox aprova sozinho; espera o webhook
     liberar o pedido e depois cancela, o que estorna o Pix.
  2. Cartão (só com MERCADOPAGO_PUBLIC_KEY): gera o token do cartão de teste como o Brick faria,
     paga com um titular "APRO" (aprovado) e cancela (estorno); depois tenta com "OTHE"
     (recusado).

Uso:
  python3 scripts/payments-sandbox.py [base_url]
  MERCADOPAGO_PUBLIC_KEY=APP_USR-... python3 scripts/payments-sandbox.py

O e-mail do visitante vem de SANDBOX_PAYER_EMAIL (padrão test_payer@example.com) e não pode ser
o e-mail da conta Mercado Pago do vendedor.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
EMAIL = os.environ.get("SANDBOX_PAYER_EMAIL", "test_payer@example.com")
PUBLIC_KEY = os.environ.get("MERCADOPAGO_PUBLIC_KEY")
# Cartão de teste Mastercard da documentação do Mercado Pago.
TEST_CARD = {"number": "5480832801033311", "cvv": "123", "month": 11, "year": 2030}
TEST_CPF = "12345678909"


def call(method, url, body=None, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for key, value in (headers or {}).items():
        req.add_header(key, value)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return response.status, json.loads(response.read() or b"null")
    except urllib.error.HTTPError as error:
        raw = error.read()
        try:
            return error.code, json.loads(raw or b"null")
        except json.JSONDecodeError:
            return error.code, raw.decode(errors="replace")


def api(method, path, body=None, headers=None):
    return call(method, BASE + path, body, headers)


def require(status, expected, what, body):
    if status != expected:
        sys.exit(f"FALHOU: {what} -> {status}: {body}")


def pick_size():
    _, page = api("GET", "/products?size=100")
    for product in page["content"]:
        if product["status"] != "ACTIVE":
            continue
        _, detail = api("GET", f"/products/{product['id']}")
        for size in detail["sizes"]:
            if size["status"] == "ACTIVE":
                return size["id"]
    sys.exit("Nenhum produto ativo com tamanho ativo no banco de dev. Cadastre um e rode de novo.")


def place(size_id, method):
    status, placed = api(
        "POST",
        "/orders",
        {
            "items": [{"type": "PRODUCT", "productSizeId": size_id, "quantity": 1}],
            "fulfillmentType": "PICKUP",
            "paymentMethod": method,
            "paymentChannel": "ONLINE",
            "guestName": "APRO",
            "guestPhone": "11999999999",
            "guestEmail": EMAIL,
        },
    )
    require(status, 201, "POST /orders", placed)
    order = placed["order"]
    print(f"  pedido {order['code']} (id {order['id']}) em {order['status']}")
    return order["id"], {"X-Order-Token": placed["trackingToken"]}


def payments(order_id, auth):
    _, body = api("GET", f"/orders/{order_id}/payments", headers=auth)
    return body


def wait_paid(order_id, auth, seconds=120):
    deadline = time.time() + seconds
    while time.time() < deadline:
        _, order = api("GET", f"/orders/{order_id}", headers=auth)
        if order.get("paid"):
            print(f"  pago: pedido em {order['status']}")
            return
        time.sleep(3)
    sys.exit(
        "FALHOU: o webhook não confirmou o pagamento. Confira a URL do webhook no painel, o"
        " evento 'Order (Mercado Pago)' e o inspetor do ngrok (http://localhost:4040)."
    )


def cancel_and_expect_refund(order_id, auth):
    status, order = api("POST", f"/orders/{order_id}/cancel", headers=auth)
    require(status, 200, "POST /orders/{id}/cancel", order)
    last = payments(order_id, auth)[-1]
    print(f"  cancelado: pedido em {order['status']}, pagamento {last['status']}")
    if last["status"] != "REFUNDED":
        print("  aviso: o estorno ainda não aparece como REFUNDED; pode chegar pelo webhook.")


def card_token(holder):
    status, body = call(
        "POST",
        f"https://api.mercadopago.com/v1/card_tokens?public_key={PUBLIC_KEY}",
        {
            "card_number": TEST_CARD["number"],
            "security_code": TEST_CARD["cvv"],
            "expiration_month": TEST_CARD["month"],
            "expiration_year": TEST_CARD["year"],
            "cardholder": {
                "name": holder,
                "identification": {"type": "CPF", "number": TEST_CPF},
            },
        },
    )
    require(status, 201, "POST /v1/card_tokens (public key)", body)
    return body["id"]


def pay_card(order_id, auth, holder):
    status, payment = api(
        "POST",
        f"/orders/{order_id}/payments",
        {
            "method": "CARD",
            "card": {
                "token": card_token(holder),
                "paymentMethodId": "master",
                "payerEmail": EMAIL,
                "identification": {"type": "CPF", "number": TEST_CPF},
            },
        },
        auth,
    )
    require(status, 201, "POST /orders/{id}/payments (CARD)", payment)
    print(f"  cartão {holder}: {payment['status']} ({payment.get('statusDetail')})")
    return payment


size_id = pick_size()

print("Pix")
order_id, auth = place(size_id, "PIX")
status, payment = api("POST", f"/orders/{order_id}/payments", {"method": "PIX"}, auth)
require(status, 201, "POST /orders/{id}/payments (PIX)", payment)
print(f"  Pix {payment['status']}, expira {payment['expiresAt']}")
wait_paid(order_id, auth)
cancel_and_expect_refund(order_id, auth)

if not PUBLIC_KEY:
    print("\nCartão pulado: defina MERCADOPAGO_PUBLIC_KEY (public key de teste) para testar.")
    sys.exit(0)

print("\nCartão aprovado")
order_id, auth = place(size_id, "CARD")
if pay_card(order_id, auth, "APRO")["status"] != "APPROVED":
    sys.exit("FALHOU: o cartão APRO não foi aprovado.")
cancel_and_expect_refund(order_id, auth)

print("\nCartão recusado")
order_id, auth = place(size_id, "CARD")
if pay_card(order_id, auth, "OTHE")["status"] != "REJECTED":
    sys.exit("FALHOU: o cartão OTHE não foi recusado.")
print("\nOK: Pix, cartão e estorno funcionaram no sandbox.")
