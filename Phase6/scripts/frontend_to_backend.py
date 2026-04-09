import sys

def main():
    if len(sys.argv) != 3:
        print("Usage: python3 frontend_to_backend.py <frontend_input> <backend_output>")
        sys.exit(1)

    infile = sys.argv[1]
    outfile = sys.argv[2]

    out_lines = []

    with open(infile, "r") as f:
        for raw in f:
            line = raw.rstrip("\n")

            if len(line) < 37:
                continue

            acct = line[0:5]
            name = line[6:26].strip()
            status = line[27:28]
            balance = line[29:37].strip()

            if name == "END_OF_FILE":
                break

            name_field = f"{name:<20}"
            balance_field = f"{balance:>8}"

            out_lines.append(f"{acct}{name_field}{status}{balance_field}0000SP")

    out_lines.append("END_OF_FILE")

    with open(outfile, "w") as f:
        for line in out_lines:
            f.write(line + "\n")

if __name__ == "__main__":
    main()