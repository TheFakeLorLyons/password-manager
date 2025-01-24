## __Lors Password Manager__

#### Updated 1/23/2024

This is a password manager that works by encrypting and decrypting csvs locally. Saved csvs can be "logged into" as though they were accounts online. As of writing this, this application requires that the server is started separately from the front end; in the future I plan to automate the build process to do both of these things with one command.

### __Installation & Running__

1. Ensure you have Clojure installed on your machine. You can follow the official installation guide.

2. Clone this repository to your local machine:

3. git clone https://github.com/TheFakeLorLyons/password-manager.git

4. cd 'password manager'

5. npm install

6. clj -m LPM.clj.routes in the terminal will begin the server

7. npm start or npx shadow-cljs watch app will begin shadow-cljs and load the app in the browser at localhost:8080

8. (One time only) Upon loading the application for the first time, it will prompt you to provide locally stored, 64 bit private keys that can be randomly generated for you, or manually entered. These are considered "symmetrical" shared keys, in that they are utilized in both encryption and decryption.

### __ToDo__

1. Implement IDs to passwords

2. Implement Certificates / Tokens for enhanced security

3. Implement password list sorting and searching

4. Expand the password generation features

5. Automate the build process to run the server and front end in one command

6. Work on browser extension extension

7. Auto-Renewing Passwords

### __Technologies Used__

- Clojure and Java interop: I use clojure for the backend with core java crypto libraries like Javax crypto and BouncyCastle in order
to implement encrpytion and decryption. I used the clojure library buddy hashers for salting and hashing and authentification/authorization. I used malli for password generation in the end, and played around with spec as well. In the end I felt that my implementation with malli looked a bit cleaner so I went with that. Compojure and Ring are used to handle the http routing.

- Clojurescript/Reagent: These are used for the front end of the application.

- Calva/WSL/VS Code: My actual coding setup and helpful packages.

- Smart git and github pages of course to host this.

- Git,yml, flowstorm debugger, more: supporting technologies! Helping me to host this and also ensure it is working correctly respectively.

###### Thank you for taking the time to look at my application and feel free to reach out with any questions -Lor