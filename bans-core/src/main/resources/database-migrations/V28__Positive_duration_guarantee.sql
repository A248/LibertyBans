
ALTER TABLE "${tableprefix}punishments"
  ADD CONSTRAINT "${tableprefix}punishment_duration_positivity"
  CHECK ("end" = 0 OR "end" > "start");
