import sys

def main():
    if len(sys.argv) != 3:
        print("Usage: python3 backend_to_frontend.py <backend_input> <frontend_output>")
        sys.exit(1)

    infile = sys.argv[1]
    outfile = sys.argv[2]

    out_lines = []

    with open(infile, "r") as f:
        for raw in f:
            line = raw.rstrip("\n")

            if line == "END_OF_FILE":
                break

            if len(line) < 40:
                continue

            acct = line[0:5]
            name = line[5:25].strip()
            status = line[25:26]
            balance = line[26:34]

            name_field = f"{name:<20}"
            balance_field = f"{balance:>8}"

            out_lines.append(f"{acct} {name_field} {status} {balance_field}")

    out_lines.append("00000 END_OF_FILE          A 00000.00")

    with open(outfile, "w") as f:
        for line in out_lines:
            f.write(line + "\n")

if __name__ == "__main__":
    main()