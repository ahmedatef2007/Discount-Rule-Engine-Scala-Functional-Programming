### Discount Engine for Retail Store

The Discount Engine for the retail store is a Java application designed to automate the discount calculation process based on specific qualifying rules. This application reads order data from a CSV file, applies discount rules, calculates final prices, and inserts the processed data into an Oracle database.

#### Features:

- **Discount Rules**: Implements various discount rules based on product types, remaining days before expiry, quantity sold, and special dates.
- **Database Interaction**: Utilizes Oracle JDBC driver to connect to a database and insert processed order data.
- **Logging Mechanisms**: Logs engine rule interactions and errors to a text file for debugging and auditing purposes.

#### How to Use:

1. **Clone Repository**:
   ```
   git clone https://github.com/your-username/discount-engine.git
   ```

2. **Import Project**: Import the project into your preferred Java IDE.

3. **Database Configuration**:
   - Update the database connection details in the code (`main.scala`) with your Oracle database URL, username, and password.

4. **Run Application**:
   - Compile and run the `main.scala` file to execute the discount calculation process.
   - Ensure that the required dependencies are installed and the CSV file containing order data (`TRX1000.csv`) is available in the specified location.

5. **Verify Results**:
   - Check the `orders` table in your Oracle database for inserted records with calculated discounts and total prices.
   - Review the `logs.txt` file for logged engine rule interactions and any error messages.


