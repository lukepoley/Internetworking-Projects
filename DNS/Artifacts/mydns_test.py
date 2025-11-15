import csv
import dns.resolver

INPUT_CSV = "top500.csv"
OUTPUT_CSV = "results.csv"

# Create resolvers
carleton = dns.resolver.make_resolver_at("137.22.1.7")
google = dns.resolver.make_resolver_at("8.8.8.8")

def query(resolver, domain):
    """Return (addresses_list, ttl) or ([], -1) on failure."""
    try:
        answers = resolver.resolve(domain, "A")
        ttl = answers.rrset.ttl
        addrs = [rdata.address for rdata in answers]
        return addrs, ttl
    except Exception:
        return [], -1


def read_domains(path):
    """Read first column of CSV into a list of domains."""
    domains = []
    with open(path, newline="") as f:
        reader = csv.reader(f)
        for row in reader:
            if row:               # non-empty row
                domains.append(row[0].strip())
    return domains


def main():
    domains = read_domains(INPUT_CSV)

    with open(OUTPUT_CSV, "w", newline="") as out:
        writer = csv.writer(out)
        writer.writerow([
            "domain_name",
            "Carleton_response_addresses",
            "Google_response_addresses",
            "TTL_Carleton",
            "TTL_Google"
        ])

        for domain in domains:
            print(f"Querying {domain} ...")

            carl_addrs, carl_ttl = query(carleton, domain)
            goog_addrs, goog_ttl = query(google, domain)

            writer.writerow([
                domain,
                ";".join(carl_addrs),
                ";".join(goog_addrs),
                carl_ttl,
                goog_ttl
            ])

    print("Output: ", OUTPUT_CSV)


if __name__ == "__main__":
    main()
