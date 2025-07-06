import { ReactNode } from 'react'

type Props = {
  children: ReactNode
}

export default function MainLayout({ children }: Props) {
  return (
    <div className="flex min-h-screen">
      {/* Sidebar */}
      <aside className="w-64 bg-gray-800 text-white p-4 hidden md:block">
        <h2 className="text-lg font-bold mb-4">Menu</h2>
        <ul className="space-y-2">
          <li>Dashboard</li>
          <li>Timesheet</li>
          <li>Clienti</li>
        </ul>
      </aside>

      {/* Main */}
      <div className="flex-1 p-6 bg-gray-100">
        {/* Header */}
        <header className="mb-6">
          <h1 className="text-2xl font-semibold">Serendipity Timesheet</h1>
        </header>

        {/* Page content */}
        <main>{children}</main>
      </div>
    </div>
  )
}
