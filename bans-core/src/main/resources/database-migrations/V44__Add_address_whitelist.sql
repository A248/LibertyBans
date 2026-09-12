
CREATE TABLE "${tableprefix}address_whitelist" (
  "address" ${inettype} NOT NULL,
  "whitelisted_on" BIGINT NOT NULL,
  "whitelisted_by" ${uuidtype} NOT NULL,
CONSTRAINT "${tableprefix}whitelist_address_uniqueness" UNIQUE ("address")
)${extratableoptions};
