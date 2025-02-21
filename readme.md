## __Lors Password Manager__

#### Updated 2/20/2024

This is a password manager that works by encrypting and decrypting csvs locally and allowing users to perform basic CRUD operations on their passwords. After having created an encrypted csv, you can 'log back into' the csv with the same credentials that were provided upon its creation.

### __Installation & Running__

1. Ensure you have Clojure installed on your machine. You can follow the official installation guide: https://clojure.org/guides/install_clojure.

2. Clone this repository to your local machine:

`git clone https://github.com/TheFakeLorLyons/password-manager.git`

3. Navigate the root directory containing deps.edn (`cd 'password manager'`).

4. If it is your first time running the application, run the CLI command `npm run install-and-start` in order to install the necessary dependencies, build the application, start the server (on port: 3000), and host the front end at http://localhost:8080/. It will take several minutes for all the required dependencies to install, the server to build, and for the front end to start. After doing this the first time, `npm run start!` will much more quickly begin the server and front end because the files will have been built already.

5. (One time only) Upon loading the application for the first time, it will prompt you to generate or manually locally stored, 64-bit public and private keys, which will be used for the encryption and decryption of passwords.

The keys will be saved, and the application will restart, allowing you to begin securely storing your passwords anywhere on your computer.

### __ToDo__

1. Implement IDs to passwords

2. Implement Certificates / Tokens for enhanced security

3. Implement password list sorting and searching

4. Expand the password generation features

✅ Automate the build process to run the server and front end in one command

6. Work on browser extension extension

7. Auto-Renewing Passwords

8. Close application after a number of minutes open

### __Technologies Used__

- Clojure and Java interop: I use clojure for the backend with core java crypto libraries like Javax crypto and BouncyCastle in order to implement encrpytion and decryption. I used the clojure library buddy hashers for salting and hashing and authentification/authorization. I used malli for password generation in the end, and played around with spec as well. In the end I felt that my implementation with malli looked a bit cleaner so I went with that. Compojure and Ring are used to handle the http routing.

- Clojurescript/Reagent: These are used for the front end of the application.

- Calva/WSL/VS Code: My actual coding setup and helpful packages.

- Smart git and github pages of course to host this.

- Git, yml, flowstorm debugger, supporting technologies! Helping me to host this and also ensure it is working correctly respectively.

###### Thank you for taking the time to look at my application and feel free to reach out with any questions -Lor