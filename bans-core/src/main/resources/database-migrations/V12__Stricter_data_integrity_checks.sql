
ALTER TABLE "${tableprefix}addresses"
  ADD CONSTRAINT "${tableprefix}address_length"
  CHECK (OCTET_LENGTH("address") IN (4, 16));

ALTER TABLE "${tableprefix}victims"
  ADD CONSTRAINT "${tableprefix}victim_address_length"
  CHECK (OCTET_LENGTH("address") IN (4, 16));
