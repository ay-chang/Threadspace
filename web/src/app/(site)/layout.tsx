import Navbar from "@/components/Navbar";

export default function MarketingLayout({ children }: { children: React.ReactNode }) {
    return (
        <>
            <Navbar />
            <main className="mx-auto max-w-6xl px-4 py-6">{children}</main>
        </>
    );
}
