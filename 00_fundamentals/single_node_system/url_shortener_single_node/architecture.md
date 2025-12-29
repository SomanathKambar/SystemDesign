## Client → REST API → Hash Generator → DB → Redirect Service

# Flow:

## 1.Client sends long URL

## 2.Server generates Base62 hash

## 3.Stores in DB

## 4.Returns short URL

## 5.Redirect uses DB lookup