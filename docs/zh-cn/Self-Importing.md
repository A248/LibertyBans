
LibertyBans allows you to switch storage backends by importing its own data.

This is accomplished by simply copying all the data from the old database to the new database.

# Self-Importing Steps

1. Backup your data. Taking backups is good practice, *always*. If you want to be extra safe, be sure that you can restore your backup, too.
2. Configure the `import.yml` with the *old database* to import from.
3. Obtain a fresh, clean database -- the *new database* you want to use. Configure the `sql.yml` with this new database.
4. Restart LibertyBans with `/libertybans restart` and then run `/libertybans import self`.
5. Wait for the self-import process to complete. Don't join the server yet.

The new database **MUST** be empty before you run the self-import process. If the new database already has data in it, the migration is very likely to fail.
  * **This means you cannot join the server until the self-import process is complete**, since joining the server will add data to the database.
  * This also means you cannot create punishments on the new database before importing.
