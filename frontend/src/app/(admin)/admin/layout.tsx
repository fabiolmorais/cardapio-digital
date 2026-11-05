import { AdminShell } from "@/features/admin/admin-shell";
import { StaffAppSetup } from "@/features/pwa/staff-app-setup";
import { staffAppMetadata, staffAppViewport } from "@/features/pwa/staff-app-metadata";

export const metadata = staffAppMetadata;
export const viewport = staffAppViewport;

export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return (
    <AdminShell>
      <StaffAppSetup />
      {children}
    </AdminShell>
  );
}
